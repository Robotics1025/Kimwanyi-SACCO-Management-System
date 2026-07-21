package org.joel.kimwanyisacco.common.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public final class FacesMessageUtil {

    private FacesMessageUtil() {
    }

    public static void addInfoMessage(String message) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, message, message));
    }

    public static void addErrorMessage(String message) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
    }
}
