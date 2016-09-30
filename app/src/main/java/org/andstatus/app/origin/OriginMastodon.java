package org.andstatus.app.origin;

class OriginMastodon extends Origin {
    @Override
    public boolean isUsernameNeededToStartAddingNewAccount(boolean isOAuthUser) {
        return false;
    }
}
