package com.cavetale.ms.session;

public enum ItemInsertionCause {
    DUMP() {
        @Override public boolean drainShulkerBoxes() {
            return false;
        }
    },
    INSERT_MENU,
    SHIFT_CLICK_MENU() {
        @Override public boolean sendChatMessage() {
            return false;
        }
        @Override public boolean sendActionBarMessage() {
            return true;
        }
    },
    PICKUP() {
        @Override public boolean drainShulkerBoxes() {
            return false;
        }
        @Override public boolean sendChatMessage() {
            return false;
        }
        @Override public boolean sendActionBarMessage() {
            return false;
        }
        @Override public boolean failSilently() {
            return true;
        }
    },
    ASSIST_CONTROL_DROP() {
        @Override public boolean sendChatMessage() {
            return false;
        }
        @Override public boolean sendActionBarMessage() {
            return true;
        }
    },
    CONTAINER_DRAIN;

    public boolean drainShulkerBoxes() {
        return true;
    }

    public boolean sendChatMessage() {
        return true;
    }

    public boolean sendActionBarMessage() {
        return false;
    }

    public boolean failSilently() {
        return false;
    }
}
