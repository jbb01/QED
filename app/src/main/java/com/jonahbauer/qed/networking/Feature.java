package com.jonahbauer.qed.networking;

public enum Feature {
    CHAT, DATABASE, GALLERY;

    @SuppressWarnings("unused")
    public static class FeatureNotSupportedException extends RuntimeException {
        public FeatureNotSupportedException() {
        }

        public FeatureNotSupportedException(String message) {
            super(message);
        }

        public FeatureNotSupportedException(String message, Throwable cause) {
            super(message, cause);
        }

        public FeatureNotSupportedException(Throwable cause) {
            super(cause);
        }

        public FeatureNotSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
