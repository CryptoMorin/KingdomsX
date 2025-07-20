package org.kingdoms.services.maps.abstraction.markersets;

import org.kingdoms.constants.group.Kingdom;

import java.util.function.Consumer;
import java.util.function.Predicate;

public enum MarkerListenerType {
    KINGDOMS, NATIONS;

    public interface Settings {
        MarkerListenerType type();
    }

    public interface IconContainer extends Settings {
        boolean showIcons();
    }

    public interface Ignorable<T> extends Settings {
        boolean isIgnored(T group);
    }

    public static final class Kingdoms implements IconContainer, Ignorable<Kingdom> {
        public boolean showLands, showIcons;
        public Consumer<Kingdom> updateListener;
        private Predicate<Kingdom> ignore;

        public boolean isIgnored(Kingdom kingdom) {
            return this.ignore != null && this.ignore.test(kingdom);
        }

        public Kingdoms updateListener(Consumer<Kingdom> updateListener) {
            this.updateListener = updateListener;
            return this;
        }

        public void onUpdate(Kingdom kingdom) {
            if (updateListener != null) updateListener.accept(kingdom);
        }

        public Kingdoms showLands(boolean showLands) {
            this.showLands = showLands;
            return this;
        }

        public Kingdoms showIcons(boolean showIcons) {
            this.showIcons = showIcons;
            return this;
        }

        public Kingdoms ignore(Predicate<Kingdom> ignore) {
            this.ignore = ignore;
            return this;
        }

        @Override
        public MarkerListenerType type() {
            return KINGDOMS;
        }

        @Override
        public boolean showIcons() {
            return showIcons;
        }
    }

    public static final class Nations implements IconContainer {
        public final boolean showLands, showIcons;

        public Nations(boolean showLands, boolean showIcons) {
            this.showLands = showLands;
            this.showIcons = showIcons;
        }

        @Override
        public MarkerListenerType type() {
            return NATIONS;
        }

        @Override
        public boolean showIcons() {
            return showIcons;
        }
    }
}
