package com.cavetale.ms.session;

public abstract sealed class SessionWorldContainerAction extends SessionAction permits SessionFillWorldContainer, SessionDrainWorldContainer {
}
