package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class FacesMessageUtilTest {

    @Test
    void addInfoMessageAddsInfoSeverityMessage() {
        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            FacesMessageUtil.addInfoMessage("Saved successfully");

            ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), captor.capture());
            assertEquals(FacesMessage.SEVERITY_INFO, captor.getValue().getSeverity());
            assertEquals("Saved successfully", captor.getValue().getSummary());
        }
    }

    @Test
    void addErrorMessageAddsErrorSeverityMessage() {
        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            FacesMessageUtil.addErrorMessage("Something went wrong");

            ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), captor.capture());
            assertEquals(FacesMessage.SEVERITY_ERROR, captor.getValue().getSeverity());
            assertEquals("Something went wrong", captor.getValue().getSummary());
        }
    }
}
