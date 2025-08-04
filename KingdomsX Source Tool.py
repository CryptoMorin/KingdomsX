#!/usr/bin/env python
# Author: Crypto Morin
# Last Update: 08/04/2025
# Version: 1.0.0

import os
import sys
import shutil
import zipfile
import subprocess
import json
import time
import re
import traceback
import logging
import inspect
import urllib.error
import concurrent.futures
from urllib.request import urlopen, Request
from typing import Tuple, Any, Dict, Callable, TypeVar, no_type_check
from logging import LogRecord
from enum import Enum

T = TypeVar('T')

# Constants that can be changed manually without editing the code itself.
SCRIPT_FILE_NAME = os.path.basename(__file__)
GRADLE_BUILD_FILE = "build.gradle.kts"
KINGDOMS_REMAPPER_FILE = "kingdoms-remapper-1.0.0.jar"
SPIGOT_VERSION = "1.21.7-R0.1-SNAPSHOT"
xseries_version = "13.3.3" # Default if can't be fetched
KINGDOMS_LATEST_VERSION = '1.17.18.1-BETA' # Currently unused
SOURCE_ZIP_FILE = "repo.zip"
GITHUB_TOKEN = None
USE_BRANCH = None # None is master/main
LOGGING_LEVEL = logging.INFO
AVAILABLE_ADDONS: Dict[str, str] = {
    'map-viewers': 'Map Viewers',
    'enginehub': 'EngineHub',
    'admin-tools': 'Admin Tools',
    'peace-treaties': 'Peace Treaties',
    'outposts': 'Outposts'
}

class GitHubCloningMethod(Enum):
    GIT = ('git', "Uses git command line interface to clone the entire repository and then extract the specific addon. (RECOMMENDED)")
    BUILT_IN = ('built-in', "Uses built-in script to download files. May get rate-limited easily.")
    DL_DIR_GH_IO = ('download-directory.github.io', "An external website that also downloads files individually, may get rate-limited easier but has a faster download speed, specially if your internet is slow.")

KINGDOMS_REAMPPER_URL = f'https://github.com/CryptoMorin/KingdomsX/raw/b3289b43114cb28c273b74aa696c753ee8b61f2d/kingdoms-remapper/{KINGDOMS_REMAPPER_FILE}'
KINGDOMS_MAVEN_BASE_URL = 'https://repo1.maven.org/maven2/com/github/cryptomorin/kingdoms/'
XSERIES_MAVEN_BASE_URL = 'https://repo1.maven.org/maven2/com/github/cryptomorin/XSeries/'

project_name: str | None = None

def replace_internal_project(project_name: str, replaced: list[str], spaces: str, scope: str, project: str, isTransitive: str | None) -> str: 
    inner_proj: str
    if project.startswith(':' + project_name):
        inner_proj = project.replace(':' + project_name, "")
    elif project.startswith(project_name):
        inner_proj = project.replace(project_name, "")
    elif 'core' in project or 'platform' in project or 'nbt' in project or 'shared' in project:
        # It's some sub-project for the :core module.
        # Removing spaces can make two statements go on the same line.
        return '\n' if len(replaced) == 0 else ''
    else:
        # We don't know what this is
        inner_proj = project
    
    if isTransitive is None: isTransitive = ""
    return f'{spaces}{scope}(project("{inner_proj}")){isTransitive}\n'

class GitHubRepository:
    def __init__(self, owner: str, name: str, branch: str | None):
        self.owner = owner
        self.name = name
        self.branch = branch

    def getDownloadDirIOURL(self, folder_path: str) -> str:
        return f"https://download-directory.github.io?url=https://github.com/{self.owner}/{self.name}/tree/{self.branch}/{folder_path}"

class GradleProcessingError(Exception):
    def __init__(self, message: str):
        super().__init__(message)

class Colors:
    """ ANSI color codes """

    BLACK = "\033[0;30m"
    RED = "\033[0;31m"
    GREEN = "\033[0;32m"
    BROWN = "\033[0;33m"
    BLUE = "\033[0;34m"
    PURPLE = "\033[0;35m"
    CYAN = "\033[0;36m"
    LIGHT_GRAY = "\033[0;37m"
    DARK_GRAY = "\033[1;30m"
    LIGHT_RED = "\033[1;31m"
    LIGHT_GREEN = "\033[1;32m"
    YELLOW = "\033[1;33m"
    LIGHT_BLUE = "\033[1;34m"
    LIGHT_PURPLE = "\033[1;35m"
    LIGHT_CYAN = "\033[1;36m"
    LIGHT_WHITE = "\033[1;37m"
    BOLD = "\033[1m"
    FAINT = "\033[2m"
    ITALIC = "\033[3m"
    UNDERLINE = "\033[4m"
    BLINK = "\033[5m"
    NEGATIVE = "\033[7m"
    CROSSED = "\033[9m"
    END = "\033[0m"

    SECONDARY = CYAN + BOLD
    ERR_SECONDARY = YELLOW + BOLD


    # cancel SGR codes if we don't write to a terminal
    if not __import__("sys").stdout.isatty():
        for _ in dir():
            # if isinstance(_, str) and _[0] != "_":
            if _[0] != "_":
                locals()[_] = ""
    else:
        # set Windows console in VT mode
        if __import__("platform").system() == "Windows":
            kernel32 = __import__("ctypes").windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
            del kernel32


class CustomFormatter(logging.Formatter):
    @staticmethod
    def _create_format(color: str) -> str: 
        # (%(filename)s:%(lineno)d)
        return f"{Colors.LIGHT_GRAY}[{Colors.DARK_GRAY}%(asctime)s{Colors.LIGHT_GRAY}][{color}%(levelname)s{Colors.LIGHT_GRAY}] {color}%(message)s{Colors.END}"


    FORMATS = {
        logging.DEBUG: _create_format(Colors.PURPLE),
        logging.INFO: _create_format(Colors.LIGHT_BLUE),
        logging.WARNING: _create_format(Colors.YELLOW),
        logging.ERROR: _create_format(Colors.RED),
        logging.CRITICAL: _create_format(Colors.RED),
        logging.DEBUG: _create_format(Colors.BLUE)
    }

    def format(self, record: LogRecord):
        log_fmt = self.FORMATS.get(record.levelno)
        formatter = logging.Formatter(log_fmt, datefmt='%I:%M:%S %p')
        return formatter.format(record)
    
logger = logging.getLogger("KingdomsX Gradle Processor")
logger.setLevel(LOGGING_LEVEL)
ch = logging.StreamHandler()
ch.setLevel(LOGGING_LEVEL)
ch.setFormatter(CustomFormatter())
logger.addHandler(ch)

def fetch_latest_maven_version(base_url: str) -> str:
    complete_url: str = base_url + 'maven-metadata.xml'
    try:
        maven_metadata: str
        with urlopen(complete_url) as response:
            file_content = response.read()
            decoded_content = file_content.decode('utf-8')
            maven_metadata = decoded_content
        
        match = re.search(r'<versioning>\s+<latest>(?P<version>[\w\d\.\-]+)</latest>', maven_metadata)
        if match is None:
            raise GradleProcessingError(f"Couldn't find the latest Kingdoms version from {complete_url} with content: {maven_metadata}")
        else:
            return match.group('version')

    except Exception as e:
        raise Exception(f'Error while downloading "{complete_url}"') from e

def raise_or_propagate(error: str, ex: Exception):
    if isinstance(ex, GradleProcessingError):
        raise GradleProcessingError(error) from ex
    else:
        logger.error(error)
        print(Colors.RED)
        raise ex

def download_from_maven(base_url: str, prefix: str, version: str, download_to: str) -> None:
    complete_url = f'{base_url}{version}/{prefix}-{version}.jar'

    try:
        with open(download_to, 'wb') as writer:
            shutil.copyfileobj(urlopen(complete_url), writer)
    except Exception as e:
        raise Exception(f'Error while downloading {complete_url}') from e
    
def download_kingdoms_remapper() -> None:
    try:
        with open(KINGDOMS_REMAPPER_FILE, 'wb') as writer:
            shutil.copyfileobj(urlopen(KINGDOMS_REAMPPER_URL), writer)
        logger.info(f"Successfully downloaded {Colors.SECONDARY}{KINGDOMS_REMAPPER_FILE}")
    except Exception as e:
        raise GradleProcessingError(f"Couldn't download kingdoms remapper from {KINGDOMS_REAMPPER_URL}") from e

fetched_xseries = False
def fetch_xseries_version() -> str:
    global fetched_xseries
    global xseries_version

    if not fetched_xseries:
        try:
            logger.info("Fetching latest XSeries version...")
            xseries_version = fetch_latest_maven_version(XSERIES_MAVEN_BASE_URL)
            logger.info(f"Using XSeries {Colors.SECONDARY}v{xseries_version}")
        except Exception:
            logger.error(f"Error while fetching XSeries latest version, using the built-in {xseries_version} known version instead.")
            print_trace()

    fetched_xseries = True
    return xseries_version

RELOCATOR = """
val LIBS = "org.kingdoms.libs"
val SHADOWED: Map<String, String> = linkedMapOf(
    "kotlin" to "$LIBS.kotlin",
    "com.github.benmanes.caffeine" to "$LIBS.caffeine",
    "com.cryptomorin.xseries" to "$LIBS.xseries",
    "org.checkerframework" to "$LIBS.checkerframework",
    "org.jetbrains" to "$LIBS.jetbrains",
    "org.intellij" to "$LIBS.intellij",
    "org.snakeyaml" to "$LIBS.snakeyaml"
)
tasks.shadowJar {
    exclude("org/jetbrains/**")
    exclude("org/intellij/**")
    exclude("kotlin/**")
    exclude("META-INF/versions/**")
    exclude("META-INF/maven/**")

    SHADOWED.forEach { (from, to) ->
        relocate(from, to) {
            exclude("%regex[org]")
        }
    }
}
"""

# List of (find, replace) tuples for multiple replacements
REPLACEMENTS: list[Tuple[str, str | Callable[..., str], bool]] = [
        (
            r"plugins\s*{([^}]+)}",
"""plugins {
    id("java")
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "9.0.0-rc3"
}""", True),

(r"""
kingdomsAddon {
    addonName\.set\("[\w-]+"\)
}
""", 
"", True),

    (
        """
tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}""",
        RELOCATOR,
        False
    ),
    (
        'compileOnly(KingdomsGradleCommons.XSERIES)',
        lambda: f'compileOnly("com.github.cryptomorin:XSeries:{fetch_xseries_version()}")',
        False
    ),
    (
       r"(?P<spaces>\s+)(?P<scope>api|implementation|compileOnly)\(project\(\"(?P<project>[\w\:\-\$]+)\"\)\)(?P<isTransitive>\s*{\s*isTransitive\s*=\s*false\s*})?\n?",
       replace_internal_project,
       True
    ),
    (
        r"import org\.kingdoms\.gradle\.[\w\.]+\n",
        "",
        True
    ),
    (
        "dependencies {\n",
        """fun findFile(directoryPath: File, pattern: String): String {
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    val dir = directoryPath.toPath()
    try {
        Files.list(dir).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) }
                .filter { regex.matches(it.fileName.toString()) }
                .findFirst()
                .map { it.absolutePathString() }
                .orElseThrow { IllegalStateException("Cannot find core jar in $dir") }
        }
    } catch (e: Exception) {
        throw IllegalStateException("Error accessing directory: $dir", e)
    }
}
dependencies {
""",
        False
    )
]

@no_type_check
def get_rate_limit_info(response) -> (str, str, str):
    """Extract rate limit information from response headers."""
    limit = response.getheader('X-RateLimit-Limit')
    remaining = response.getheader('X-RateLimit-Remaining')
    reset_time = response.getheader('X-RateLimit-Reset')
    return limit, remaining, reset_time

def make_request(url: str, retries: int=3, wait_time: int=60):
    """Make an API request with retry logic for rate limits."""
    req = Request(url)
    req.add_header('Accept', 'application/vnd.github.v3+json')
    if GITHUB_TOKEN is not None:
        req.add_header('Authorization', f'token {GITHUB_TOKEN}')
    
    for _ in range(retries):
        try:
            with urlopen(req) as response:
                if response.getcode() != 200:
                    raise GradleProcessingError(f"HTTP {response.getcode()}")
                limit, remaining, reset_time = get_rate_limit_info(response)  # type: ignore
                logger.debug(f"Rate limit: {remaining}/{limit} requests remaining")
                return json.loads(response.read().decode('utf-8')) if 'contents' in url else response.read(), response
        except urllib.error.HTTPError as e:
            if e.code == 403 and 'X-RateLimit-Remaining' in e.headers and int(e.headers['X-RateLimit-Remaining']) == 0:
                reset_time = int(e.headers['X-RateLimit-Reset'])
                wait = max(1, reset_time - int(time.time()) + 5)  # Add 5 seconds buffer
                logger.warning(f"Rate limit exceeded. Waiting {wait} seconds until reset...")
                time.sleep(wait)
                continue
            raise
        except urllib.error.URLError as e:
            logger.error(f"URL Error: {e.reason}")
            raise
    raise Exception("Max retries exceeded for rate limit")

def download_file(item: Dict[str, str], local_path: str):
    """Download a single file and save it to the local path."""
    try:
        logger.debug(f"Downloading file: {item['path']}")
        file_data, _ = make_request(item['download_url'])
        with open(local_path, 'wb') as f:
            f.write(file_data)
    except Exception as e:
        raise GradleProcessingError(f"Failed to download {item['path']}: {str(e)}")

def download_github_folder(repo: GitHubRepository, folder_path: str, output_dir: str='.', max_workers: int=5):
    """
    Download a specific folder from a GitHub repository with parallel file downloads.
    
    Args:
        repo_owner (str): GitHub username or organization
        repo_name (str): Repository name
        folder_path (str): Path to the folder in the repository
        branch (str): Branch name (default: 'main')
        output_dir (str): Local directory to save the downloaded files
        max_workers (int): Number of concurrent download threads (default: 5)
    """
    if GITHUB_TOKEN is None:
        alt_url = repo.getDownloadDirIOURL(folder_path)
        logger.warning("Warning: GITHUB_TOKEN is not set. Using unauthenticated requests (60 requests/hour limit).")
        logger.warning(f"If you're getting rate-limited too often, you can try using {Colors.GREEN}{Colors.BOLD}{Colors.UNDERLINE}{alt_url}{Colors.RED} {Colors.YELLOW}instead.")

    download_github_folder0(repo, folder_path=folder_path, output_dir=output_dir, max_workers=max_workers)

def download_github_folder0(repo: GitHubRepository, folder_path: str, output_dir: str, max_workers: int):
    # Construct the GitHub API URL for the folder
    api_url = f'https://api.github.com/repos/{repo.owner}/{repo.name}/contents/{folder_path}?ref={repo.branch}'
    
    try:
        # Fetch folder contents
        data, _ = make_request(api_url)
        
        # Ensure output directory exists
        os.makedirs(output_dir, exist_ok=True)
        
        # Separate files and directories
        files = [item for item in data if item['type'] == 'file']
        directories = [item for item in data if item['type'] == 'dir']
        
        # Download files in parallel
        if files:
            logger.debug(f"Downloading {len(files)} files in parallel...")
            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                futures = [
                    executor.submit(download_file, item, os.path.join(output_dir, item['name']))
                    for item in files
                ]
                concurrent.futures.wait(futures)
        
        # Recursively process subdirectories
        for item in directories:
            item_name = item['name']
            item_path = os.path.join(folder_path, item_name)
            logger.debug(f"Entering directory: {item_path}")
            download_github_folder0(
                repo,
                item_path,
                os.path.join(output_dir, item_name),
                max_workers
            )
    
    except Exception as e:
        logger.error(f"Error: {str(e)}")

def clone_repository(repo: GitHubRepository, destination_dir: str):
    """
    Clone a GitHub repository using the 'git clone' command.
    
    Args:
        repo_url (str): URL of the repository to clone (e.g., https://github.com/owner/repo.git)
        destination_dir (str): Directory to clone the repository into (optional)
        branch (str): Branch to clone (optional)
    """
    try:
        # Ensure git is installed
        result = subprocess.run(['git', '--version'], capture_output=True, text=True)
        if result.returncode != 0:
            raise GradleProcessingError("Git is not installed or not found in PATH")
        
        # Construct the git clone command
        cmd = ['git', 'clone']
        if repo.branch:
            cmd.extend(['--branch', repo.branch])

        repo_url = f'https://github.com/{repo.owner}/{repo.name}.git'
        cmd.append(repo_url)
        if destination_dir:
            cmd.append(destination_dir)
        
        # Normalize destination path if provided
        if destination_dir:
            destination_dir = os.path.abspath(destination_dir)
            os.makedirs(destination_dir, exist_ok=True)
        
        # Run the git clone command
        logger.info(f"Cloning repository: {Colors.SECONDARY}{repo_url}")
        if repo.branch:
            logger.info(f"  * Branch: {Colors.SECONDARY}{repo.branch}")
        if destination_dir:
            logger.info(f"  * Destination: {Colors.SECONDARY}{destination_dir}")
        
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode == 0:
            repo_name = repo_url.rstrip('.git').split('/')[-1]
            clone_path = os.path.join(destination_dir or os.getcwd(), repo_name)
            logger.info(f"Successfully cloned repository to '{clone_path}'")
        else:
            raise Exception(f"Git clone failed: {result.stderr.strip()}")
    
    except FileNotFoundError:
        raise GradleProcessingError("Error: Git is not installed or not found in PATH")
    except subprocess.SubprocessError as e:
        raise GradleProcessingError(f"Error: {str(e)}") from e
    except Exception as e:
        raise GradleProcessingError(f"Error: {str(e)}") from e

def find_gradle_modules(root_dir: str) -> list[str]:
    """
    Collects names of folders containing a 'build.gradle.kts' file.
    
    Args:
        root_dir: The root directory to start the search.
    """
    gradle_folders: list[str] = []
    try:
        for dirpath, _, filenames in os.walk(root_dir):
            if dirpath != root_dir and "build.gradle.kts" in filenames:
                # Get the relative path from root_dir
                rel_path = os.path.relpath(dirpath, root_dir)
                gradle_folders.append(rel_path)
        return gradle_folders
    except Exception as e:
        print(f"Error while searching directories: {str(e)}")
        return []

def normalize_gradle_project_path(project_path: str) -> str: 
    return project_path.replace(os.sep, ':')

def create_settings_gradle(buildGradle: str):
    file_name = "settings.gradle.kts"

    # Might already be in cache after downloading the project.
    global project_name
    if project_name is None:
        matcher = re.search(r'addonName.set\("(?P<projName>[\w-]+)"\)', buildGradle)
        if matcher is None:
            raise GradleProcessingError("No match found for the project's name")
        project_name = matcher.group("projName").lower()
    
    gradle_folders = [ normalize_gradle_project_path(folder) for folder in find_gradle_modules(".") ]
    content = f'rootProject.name = "{project_name}"\n\n'
    content += "\n".join([f"include(\"{folder}\")" for folder in gradle_folders])
    logger.info(f"Detected project name: {Colors.SECONDARY}{project_name}")

    try:
        with open(file_name, 'w') as file:
            file.write(content)
        logger.info(f"Successfully created {Colors.SECONDARY}{file_name}")
    except Exception as e:
        raise Exception(f"An error occurred while writing to {file_name}") from e

def create_gitignore():
    file_name = ".gitignore"
    content = f"""# Windows thumbnail cache files
Thumbs.db
ehthumbs.db
ehthumbs_vista.db

# Folder config file
Desktop.ini

# Recycle Bin used on file shares
$RECYCLE.BIN/

# Windows Installer files
*.cab
*.msi
*.msm
*.msp

# Windows shortcuts
*.lnk

# =========================
# Operating System Files
# =========================

*.iml
.idea/
.gradle/
target/
repo/
out/
**/build

# =========================
# KingdomsX Source Tool
# =========================

master.zip
{SOURCE_ZIP_FILE}
{SCRIPT_FILE_NAME}
{KINGDOMS_REMAPPER_FILE}
*.jar
.gitignore
**/{GRADLE_BUILD_FILE}
**/{GRADLE_BUILD_FILE}.bak
settings.gradle.kts
"""

    try:
        with open(file_name, 'w') as file:
            file.write(content)
        logger.info(f"Successfully created {Colors.SECONDARY}{file_name}")
    except Exception as e:
        raise Exception(f"An error occurred while writing to {file_name}") from e

def create_backup(file_path: str):
    try:
        backup_file = "build.gradle.kts.bak"
        shutil.copy2(file_path, backup_file)
        logger.info(f"Backup created for: {Colors.SECONDARY}{file_path}")
    except Exception as e:
        raise Exception(f"Error creating backup of {file_path}") from e

def collect_gradle_plugins(content: str) -> list[str]:
    matcher = re.search(r'plugins\s*{([^}]+)}', content)
    if matcher is None:
        return []

    plugin_list = matcher.group(1)
    plugin_list = plugin_list.splitlines()
    plugin_list = [ plugin.strip() for plugin in plugin_list ]
    return [ plugin for plugin in plugin_list if not plugin.startswith('//') and not len(plugin) <= 1 ]

def flat_map(nested_list: list[list[T]]) -> list[T]:
    return [item for sublist in nested_list for item in sublist]

def print_trace():
    print(Colors.RED)
    print(traceback.format_exc())
    print(Colors.END)

def has_project() -> bool:
    return os.path.isfile(GRADLE_BUILD_FILE) or os.path.isfile(SOURCE_ZIP_FILE)

def process_build_file(file_path: str, create_settings: bool):
    if not os.path.isfile(file_path):
        raise GradleProcessingError(f"{file_path} not found in current directory.")

    try:
        normalized_header = "[KingdomsX Normalized Gradle Build File]"

        # Read the file content
        with open(file_path, 'r') as file:
            content = file.read()

        if normalized_header in content:
            raise GradleProcessingError("This project is already processed by this tool.")

        # Create backup before modification
        create_backup(file_path)

        if create_settings: 
            create_settings_gradle(content)

        plugins = collect_gradle_plugins(content)
        logger.info(f"Plugin list for {Colors.SECONDARY}{file_path} {Colors.LIGHT_BLUE}-> {Colors.SECONDARY}{str(plugins)}")
        if len(plugins) == 0:
            raise GradleProcessingError("Plugin list is empty")

        repositories: list[str] = [ 'mavenLocal()', 'mavenCentral()' ]
        dependencies: list[str] = []

        if "spigotapi" in plugins:
            repositories.append(inspect.cleandoc("""
                maven {
                    url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
                    content {
                        includeGroup("org.spigotmc")
                    }
                }
            """))
            dependencies.append(f'compileOnly("org.spigotmc:spigot-api:{SPIGOT_VERSION}")')

        if "spigot" in plugins:
            repositories.append(inspect.cleandoc("""
                maven {
                    url = uri("https://repo.codemc.org/repository/nms/")
                    content {
                        includeGroup("org.spigotmc")
                    }
                }
            """))
            dependencies.append(f'compileOnly("org.spigotmc:spigot:{SPIGOT_VERSION}")')

        if "sublibs" in plugins:
            dependencies.append('compileOnly("org.checkerframework:checker-qual:3.21.0")')
            dependencies.append('compileOnly("com.google.code.gson:gson:2.10.1")')
            dependencies.append('compileOnly("com.google.guava:guava:33.1.0-jre")')
            dependencies.append('compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.2")')
            dependencies.append('compileOnly("org.jetbrains:annotations:26.0.2")')

        if 'addon' in plugins or re.search(r'compileOnly\(project\(":?(core(:[\w:]+)?|shared|nbt|platform)"\)\)', content, re.IGNORECASE):
            dependencies.append(f'compileOnly(files(rootDir.resolve("{remapped_kingdoms_jar}")))')

            # The remapped kingdoms JAR already contains the correct snakeyaml library.
            # dependencies.append(r'compileOnly(files(findFile(rootDir, "snakeyaml.+\\\.jar")))')

        # Perform all replacements
        modified_content = content
        modified_content = "" \
            "// Please don't remove this comment.\n" \
            f"// {normalized_header}\n\n" \
            "import java.nio.file.Files\n" \
            "import java.nio.file.Paths\n" \
            "import kotlin.io.path.absolutePathString\n" \
            "import kotlin.use\n\n" + content
        
        for find_str, replacement, is_regex in REPLACEMENTS:
            is_callable = callable(replacement)
            if is_regex:
                if is_callable:  
                    # Get named groups from the regex
                    regex = re.compile(find_str)
                    named_groups = regex.groupindex.keys()
                    if not named_groups:
                        raise ValueError("Pattern must contain at least one named capturing group")

                    # Get parameter names of the replacement function
                    sig = inspect.signature(replacement)
                    func_params = list(sig.parameters.keys())

                    # Check if function parameters correspond to named groups
                    missing_params = set(named_groups) - set(func_params)
                    if missing_params:
                        raise ValueError(f"Function missing parameters for groups: {missing_params}")

                    replaced: list[str] = []
                    def rep(match: re.Match[str]) -> str:
                        """
                        Replacement function for re.sub that calls replacement_func with matched groups.
                        """
                        # Create a dictionary mapping group names to their values
                        group_dict = {name: match.group(name) for name in named_groups}
                        # Prepare kwargs for the function call, only including parameters that match group names
                        kwargs = {param: group_dict[param] for param in func_params if param in group_dict}

                        if 'project_name' in func_params:
                            global project_name
                            kwargs["project_name"] = project_name
                        if 'replaced' in func_params:
                            kwargs["replaced"] = replaced

                        # Call the replacement function with the mapped arguments
                        # Pylance is too fucking stupid for callable(...)
                        raw_replacement: str = replacement(**kwargs) # type: ignore
                        replaced.append(raw_replacement) # type: ignore
                        return raw_replacement # type: ignore
                    
                    modified_content = regex.sub(rep, modified_content)
                else:
                    modified_content = re.sub(find_str, replacement, modified_content)
            else:
                # Use plain string replacement
                final_replacement = replacement() if is_callable else replacement
                modified_content = modified_content.replace(find_str, final_replacement)

        final_repos = r"repositories {\n    " + "\n    ".join(flat_map( [ repo.splitlines() for repo in repositories ])) + '\n'
        if "repositories {" in modified_content:
            final_repos = r'\1' + final_repos
            modified_content = re.sub(r"(\s+)repositories {\n", final_repos, modified_content)
        else:
            final_repos = r'\1' + '\n\n' + final_repos + '}'
            modified_content = re.sub(r"(\s+plugins {\n[^}]+})", final_repos, modified_content)

        # Happens when only :core is imported.
        xseries_dep = f'compileOnly("com.github.cryptomorin:XSeries:{fetch_xseries_version()}")'
        if re.search(xseries_dep, modified_content) is None:
            dependencies.append(xseries_dep)

        final_deps = r"dependencies {\n    " + "\n    ".join(flat_map( [ dep.splitlines() for dep in dependencies ])) + '\n'
        if "dependencies {" in modified_content:
            final_deps = r"\1" + final_deps
            modified_content = re.sub(r"(\s+)dependencies {\n", final_deps, modified_content)
        else:
            final_deps += '}'
            modified_content += final_deps

        modified_content += "\n\n"
        modified_content += inspect.cleandoc("""
            java {
                val buildVersion = JavaVersion.VERSION_1_8
                disableAutoTargetJvm()
                sourceCompatibility = buildVersion
                targetCompatibility = buildVersion
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                compilerOptions {
                    val kotlinVer = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
                    languageVersion.set(kotlinVer)
                    apiVersion.set(kotlinVer)
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
                }
            }
        """)

        if not RELOCATOR in modified_content:
            modified_content += "\n\n" + RELOCATOR

        # Write final resource processing
        global project_name
        if project_name is None:
            raise GradleProcessingError("Project name should not be unknown at this point")

        modified_content += "\n\n"
        modified_content += inspect.cleandoc(f"""
            tasks.processResources {{
                val version = project.version
                val description = project.description

                filesMatching("plugin.yml") {{
                    expand(
                        mapOf(
                            "project" to mapOf(
                                "version" to version,
                                "description" to description,
                                "name" to "Kingdoms-Addon-{AVAILABLE_ADDONS[project_name]}"
                            )
                        )
                    )
                }}
            }}
        """)

        # Write the modified content back to the file
        with open(file_path, 'w') as file:
            file.write(modified_content)

        # Notify user of success
        logger.info(f"Successfully modified {Colors.SECONDARY}{file_path} {Colors.LIGHT_BLUE}with {Colors.SECONDARY}{len(REPLACEMENTS)} replacement(s)")

    except Exception as e:
        raise_or_propagate(f"An error while processing build file {Colors.SECONDARY}{file_path}", e)

def find_files(dir: str, file_pattern: str) -> list[str]:
    pattern = re.compile(file_pattern, re.IGNORECASE)
    matching_files: list[str] = []   

    for filename in os.listdir(dir):
        if pattern.match(filename):
            matching_files.append(filename)

    return matching_files

def check_kingdoms_jar():
    matching_files = find_files(os.getcwd(), r'kingdoms.*(?<!-remapped)\.jar')
    matching_files.remove(KINGDOMS_REMAPPER_FILE)
    global kingdoms_jar

    if len(matching_files) == 0:
        logger.warning(f"No Kingdoms JARs found to import. Fetching the latest Kingdoms Jar from Maven...")
        latest_kingdoms = fetch_latest_maven_version(KINGDOMS_MAVEN_BASE_URL)
        logger.info(f'Downloading Kingdoms version {Colors.SECONDARY}{latest_kingdoms} {Colors.LIGHT_BLUE}from Maven...')
        download_path = f'kingdoms-{latest_kingdoms}.jar'
        download_from_maven(KINGDOMS_MAVEN_BASE_URL, 
                                         prefix='kingdoms', 
                                         version=latest_kingdoms, 
                                         download_to=download_path)
        logger.info(f'Downloaded {Colors.SECONDARY}{download_path}')
        kingdoms_jar = download_path
    elif len(matching_files) != 1:
        raise GradleProcessingError(f"Found more than one matching kingdoms JAR {Colors.ERR_SECONDARY}{matching_files}{Colors.RED} please remove the unnecessary ones to prevent issues.")
    else:
        kingdoms_jar = matching_files[0]
        logger.info(f"Using kingdoms JAR: {Colors.LIGHT_CYAN}{Colors.BOLD}{kingdoms_jar}")

def remap_kingdoms_jar():
    if not os.path.isfile(KINGDOMS_REMAPPER_FILE):
        raise GradleProcessingError(f"Couldn't find the remapper jar {Colors.ERR_SECONDARY}'{KINGDOMS_REMAPPER_FILE}' {Colors.RED}in the current directory")

    logger.info(f"Using kingdoms remapper {Colors.SECONDARY}{KINGDOMS_REMAPPER_FILE}")

    global kingdoms_jar
    global remapped_kingdoms_jar

    remapped_kingdoms_jar = os.path.splitext(os.path.basename(kingdoms_jar))[0] + "-remapped.jar"
    java_command = ["java", "-jar", KINGDOMS_REMAPPER_FILE, kingdoms_jar, remapped_kingdoms_jar]

    try:
        # Run the Java application as a subprocess
        # capture_output=True captures stdout and stderr
        # text=True decodes output as text
        logger.info(f"Running kingdoms remapper with: {Colors.SECONDARY}{' '.join(java_command)}")
        result = subprocess.run(java_command, capture_output=True, text=True, check=True)

        # Access standard output and error
        if result.stdout.strip() != '':
            logger.info("Java Application Output:")
            logger.info(result.stdout)

        if result.stderr.strip() != '':
            logger.error("Java Application Errors:")
            logger.error(result.stderr)

    except subprocess.CalledProcessError as e:
        raise GradleProcessingError(f"Error running Java application: {e}\n{e.stderr}")
    except FileNotFoundError:
        raise GradleProcessingError("Java executable not found. Ensure Java is installed and in your PATH.")

def delete_project():
    """
    Deletes the project and cache files and unzips them again from {SOURCE_ZIP_FILE}
    """

    current_dir = os.getcwd()
    deleted_items: list[str] = []
    errors: list[str] = []

    script_filename = os.path.basename(__file__)
    excluding = [ 
        '.gradle', '.idea', SOURCE_ZIP_FILE, 
        KINGDOMS_REMAPPER_FILE, 
        script_filename, 
        r'kingdoms.*(?<!-remapped)\.jar', 
        r'snakeyaml[\w\-\.]+\.jar' 
    ]

    compiled_patterns: list[re.Pattern[str]] = []
    for pattern in excluding:
        try:
            compiled_patterns.append(re.compile(pattern, re.IGNORECASE))
        except re.error as e:
            errors.append(f"Invalid regex pattern '{pattern}': {str(e)}")

    if errors:
        print("\nErrors in regex patterns:")
        for error in errors:
            print(f"- {error}")
        return

    # Get all items in the current directory
    for item in os.listdir(current_dir):
        # Skip if item matches any regex pattern in the whitelist
        if any(pattern.match(item) for pattern in compiled_patterns):
            continue
        
        item_path = os.path.join(current_dir, item)
        
        try:
            if os.path.isdir(item_path):
                delete_folder(item_path)
                deleted_items.append(f"Directory: {Colors.SECONDARY}{item}")
            elif os.path.isfile(item_path):
                os.remove(item_path)
                deleted_items.append(f"File: {Colors.SECONDARY}{item}")
        except Exception as e:
            errors.append(f"Error deleting {item}: {Colors.ERR_SECONDARY}{str(e)}")

    # Print results
    if deleted_items:
        logger.info("Successfully deleted:")
        deleted_items.sort()
        for item in deleted_items:
            logger.info(f"  * {item}")
    if errors:
        logger.error("Errors encountered:")
        errors.sort()
        for error in errors:
            logger.error(f"  * {error}")
        raise GradleProcessingError("Error while resetting project")

def zip_folder(folder_path: str, output_zip: str, include_folder_itself: bool = False):
    """
    Create a ZIP file from a folder and its contents.
    
    Args:
        folder_path (str): Path to the folder to zip
        output_zip (str): Path for the output ZIP file
    """
    try:
        # Ensure the folder exists
        if not os.path.isdir(folder_path):
            raise GradleProcessingError(f"Error: '{folder_path}' is not a valid directory")
        
        # Normalize paths
        folder_path = os.path.abspath(folder_path)
        output_zip = os.path.abspath(output_zip)
        
        # Create a ZIP file
        with zipfile.ZipFile(output_zip, 'w', zipfile.ZIP_DEFLATED) as zipf:
            # Walk through the folder
            for root, _, files in os.walk(folder_path):
                for file in files:
                    if file == root: continue
                    file_path = os.path.join(root, file)
                    # Calculate the relative path for the ZIP file

                    if include_folder_itself:
                        relative_path = os.path.join(folder_path, os.path.relpath(file_path, folder_path))
                    else:
                        relative_path = os.path.relpath(file_path, folder_path)
    
                    logger.debug(f"Adding: {relative_path}")
                    # Add file to the ZIP
                    zipf.write(file_path, relative_path)
        
        logger.info(f"Successfully created ZIP file: {output_zip}")
    
    except Exception as e:
        raise Exception(f"Error while zipping {folder_path} -> {output_zip}") from e

def unzip_file(zip_filename: str):
    """
    Unzip the specified ZIP file to the current directory.
    
    Args:
        zip_filename (str): Name of the ZIP file to extract
    """
    current_dir = os.getcwd()

    # Check if the ZIP file exists
    zip_path = os.path.join(current_dir, zip_filename)
    if not os.path.isfile(zip_path):
        logger.error(f"ZIP file {Colors.ERR_SECONDARY}'{zip_filename}' {Colors.RED}not found in current directory")
        return

    errors: list[str] = []
    try:
        # Open and extract the ZIP file
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(current_dir)

        logger.info(f"Successfully extracted {Colors.SECONDARY}'{zip_filename}'{Colors.LIGHT_BLUE} to {Colors.SECONDARY}{current_dir} " \
                    f"{Colors.LIGHT_BLUE}with a total of {Colors.SECONDARY}{len(zip_ref.namelist())} {Colors.LIGHT_BLUE}entries.")
        
        # List extracted files and directories
        # extracted_items = zip_ref.namelist()
        # if extracted_items:
            # for item in extracted_items:
            #     logger.info(f"- {item}")
    
    except zipfile.BadZipFile:
        errors.append(f"'{zip_filename}' is not a valid ZIP file")
    except PermissionError:
        errors.append(f"Permission denied while extracting '{zip_filename}'")
    except Exception as e:
        errors.append(f"Error extracting '{zip_filename}': {str(e)}")

    # Print any errors
    if errors:
        logger.error("Errors encountered:")
        for error in errors:
            logger.error(f"  * {error}")
        raise GradleProcessingError("Error while extracting files from zip")

def copy_folder(source_folder: str, destination_folder: str):
    """
    Copy all contents from source folder to destination folder.
    
    Args:
        source_folder (str): Path to the source folder
        destination_folder (str): Path to the destination folder
    """
    try:
        # Ensure source folder exists
        if not os.path.isdir(source_folder):
            raise Exception(f"Error: '{source_folder}' is not a valid directory")
        
        # Ensure destination folder exists or create it
        os.makedirs(destination_folder, exist_ok=True)
        
        # Normalize paths
        source_folder = os.path.abspath(source_folder)
        destination_folder = os.path.abspath(destination_folder)
        
        # Copy entire folder contents recursively
        for item in os.listdir(source_folder):
            source_item = os.path.join(source_folder, item)
            destination_item = os.path.join(destination_folder, item)
            
            if os.path.isfile(source_item):
                logger.debug(f"Copying file: {source_item} -> {destination_item}")
                shutil.copy2(source_item, destination_item)
            elif os.path.isdir(source_item):
                logger.debug(f"Copying directory: {source_item} -> {destination_item}")
                shutil.copytree(source_item, destination_item, dirs_exist_ok=True)
        
        logger.info(f"Successfully copied '{source_folder}' to '{destination_folder}'")
    
    except Exception as e:
        raise Exception(f"Error while copying {source_folder} -> {destination_folder}") from e

def ask_until_valid(validator: Callable[[str], T | None]) -> T:
    while True:
        answer = input()
        validated = validator(answer)
        if validated is not None: 
            return validated

def delete_folder(folder: str):
    # Remove read-only attribute from files
    # Mostly useful for files created by .git folder.
    # https://stackoverflow.com/questions/76546956/permissionerror-winerror-5-access-is-denied-team-sortifie-git-git-object
    for root, dirs, files in os.walk(folder):
        for d in dirs:
            os.chmod(os.path.join(root, d), 0o777)
        for f in files:
            os.chmod(os.path.join(root, f), 0o777)
    shutil.rmtree(folder)

def confirmed_process_project():
    if not os.path.isfile(KINGDOMS_REMAPPER_FILE):
        logger.warning(f"Couldn't find the remapper jar '{KINGDOMS_REMAPPER_FILE}' in the current directory, downloading it...")
        download_kingdoms_remapper()

    if not has_project():
        logger.info("You don't seem to have an addon in the current directory, which one would you like to download?")
        [ logger.info(f"  {Colors.DARK_GRAY}* {Colors.GREEN}{addon}") for addon in AVAILABLE_ADDONS.values() ]

        def addon_name_validator(input: str) ->  str | None:
            addon_name: str = input.lower().replace(' ', '-').replace('_', '-')
            if not addon_name in AVAILABLE_ADDONS:
                logger.error(f'Unknown addon name {Colors.ERR_SECONDARY}{addon_name}{Colors.RED}, please try again:')
                return None
            else:
                return addon_name

        global project_name
        project_name = ask_until_valid(addon_name_validator)

        logger.info("Which method would you like to download this project with?")
        [ logger.info(f"  {Colors.DARK_GRAY}* {Colors.GREEN}{method.value[0]} {Colors.DARK_GRAY}- {Colors.LIGHT_BLUE}{method.value[1]}") for method in GitHubCloningMethod ]

        def method_validator(input: str) -> GitHubCloningMethod | None:
            cloning_method: str = input.lower().replace(' ', '-').replace('_', '-')
            display_names: Dict[str, GitHubCloningMethod] = { meth.value[0]: meth for meth in GitHubCloningMethod }

            if not cloning_method in display_names:
                logger.error(f'Unknown cloning method {Colors.ERR_SECONDARY}{cloning_method}{Colors.RED}, please try again:')
                return None
            else:
                return display_names[cloning_method]
            
        method = ask_until_valid(method_validator)
        repo = GitHubRepository(owner='CryptoMorin', name='KingdomsX', branch=USE_BRANCH)

        if method == GitHubCloningMethod.BUILT_IN:
            output_folder = 'repo'
            download_github_folder(repo, folder_path='enginehub', output_dir=f'./{output_folder}')

            logger.info(f"Zipping the addon folder as a backup...")
            zip_folder(output_folder, SOURCE_ZIP_FILE)

            logger.info(f"Copying addon content to root directory...")
            copy_folder(output_folder, '.')

            logger.info(f"Deleting repo folder...")
            delete_folder(output_folder)
        elif method == GitHubCloningMethod.GIT:
            clone_repository(repo, repo.branch or 'master')
            addon_folder = f'master/{project_name}'
            logger.info(f"Copying addon files from {Colors.SECONDARY}{addon_folder}")
            copy_folder(addon_folder, '.')
            logger.info("Creating backup of addon files...")
            zip_folder(addon_folder, SOURCE_ZIP_FILE)
            zip_folder('./master', 'master.zip')
            delete_folder('master')
        elif method == GitHubCloningMethod.DL_DIR_GH_IO:
            logger.info(f"You can download the project using the following link {Colors.SECONDARY}{repo.getDownloadDirIOURL(project_name)}")
            logger.info(f"After downloading, rename the zip file {Colors.SECONDARY}'repo.zip' {Colors.LIGHT_BLUE}and place it in the current folder, and type 'reset' when given the choice to change this project.")
            return
    
    check_kingdoms_jar()
    remap_kingdoms_jar()

    create_gitignore()
    process_build_file(GRADLE_BUILD_FILE, create_settings=True)
    for module in find_gradle_modules("."):
        process_build_file(module + os.sep + GRADLE_BUILD_FILE, create_settings=False)

def process_project():
    # Prompt user for confirmation
    if has_project():
        logger.info(f"{Colors.PURPLE}Are you sure you want to modify this project's build files? {Colors.END}({Colors.GREEN}y{Colors.LIGHT_GRAY}/{Colors.RED}n{Colors.END}): ")
        confirm = input().lower()

        if confirm == 'resety':
            delete_project()
            unzip_file(SOURCE_ZIP_FILE)
            print('\n\n\n')
            confirmed_process_project()
            return

        if confirm == 'reset':
            delete_project()
            unzip_file(SOURCE_ZIP_FILE)
            return
        
        if confirm == 'delete':
            delete_project()
            return

        if confirm != 'y':
            global cancelled
            cancelled = True
            logger.info("Operation cancelled.")
            return
    
    confirmed_process_project()

def safety_lock():
    current_dir = os.getcwd()
    files = os.listdir(current_dir)
    if len(files) > 30 or 'api-gen' in files:
        raise GradleProcessingError("Safety mechanism triggered. If you see this, it probably means you didn't isolate the project properly in a separate folder.")

def check_python_version():
    def str_version(version: tuple[Any, ...] ) -> str:
        return 'v' + '.'.join([ str(component) for component in version ])

    # f-strings are used extensively, so we need at least v3.6.0
    ver = sys.version_info
    REQUIRED_VERSION = (3, 6, 0)

    if ver < REQUIRED_VERSION:
        raise GradleProcessingError(f"Outdated Python version {Colors.ERR_SECONDARY}{str_version(ver)}{Colors.RED}. " \
                                    f"You need at least Python {Colors.ERR_SECONDARY}{str_version(REQUIRED_VERSION)}")

if __name__ == "__main__":
    global cancelled
    cancelled = False

    try:
        check_python_version()
        safety_lock()
        process_project()
        if not cancelled: 
            logger.info(f"{Colors.GREEN}You should now be able to run Gradle on this project.")
    except GradleProcessingError as ex:
        logger.error(str(ex))

        cause: BaseException | None = ex
        while True:
            cause = cause.__cause__
            if cause is None: break

            logger.error(f"  {Colors.DARK_GRAY}* {Colors.RED}{str(cause)}")

    except Exception:
        logger.error("Failed to process project")
        print_trace()
    except KeyboardInterrupt:
        logger.info("Operation cancelled by keyboard.")

    logger.info(f"{Colors.PURPLE}Press {Colors.UNDERLINE}Enter{Colors.PURPLE} to exit...{Colors.END}")
    input()