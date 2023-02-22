# https://pyyaml.org/wiki/PyYAMLDocumentation
import os

import yaml

crowdinFiles = []

for subdir, _, files in os.walk(r"core/src/main/resources/guis"):
    for file in files:
        subdir = subdir.replace('\\', '/')
        filepath = subdir + '/' + file
        filepath = filepath[filepath.index("guis") + len("guis") + 1:]

        if filepath.endswith(".yml"):
            crowdinFiles.append({
                "source": subdir + '/' + filepath,
                "translation": f"/resources/languages/%two_letters_code%/guis/{filepath}"
            })
            print(filepath)

with open(r'crowdin.yml', 'w') as file:
    yaml.dump(dict(files=crowdinFiles), file)
