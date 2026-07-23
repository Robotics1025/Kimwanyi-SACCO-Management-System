package org.joel.kimwanyisacco.controller;

import java.util.List;

import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.UserAccountDto;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.joel.kimwanyisacco.service.UserManagementService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.annotation.PostConstruct;

@Component("userManagementBean")
@RequestScope
public class UserManagementBean {

    private final UserManagementService userManagementService;
    private final UserSessionBean userSessionBean;
    private final UserAccountRepository userAccountRepository;

    private String searchKeyword;
    private List<UserAccountDto> results;
    
    // For password reset popup/dialog
    private String newPassword;
    private Long selectedUserId;

    public UserManagementBean(UserManagementService userManagementService,
                              UserSessionBean userSessionBean,
                              UserAccountRepository userAccountRepository) {
        this.userManagementService = userManagementService;
        this.userSessionBean = userSessionBean;
        this.userAccountRepository = userAccountRepository;
    }

    @PostConstruct
    public void init() {
        search();
    }

    public void search() {
        results = userManagementService.search(searchKeyword);
    }

    public void toggleEnabled(Long userId, boolean currentlyEnabled) {
        try {
            UserAccount admin = getActingAdmin();
            userManagementService.setEnabled(userId, !currentlyEnabled, admin);
            search(); // refresh list
            FacesMessageUtil.addInfoMessage("User status updated successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public void resetPassword() {
        try {
            UserAccount admin = getActingAdmin();
            userManagementService.resetPassword(selectedUserId, newPassword, admin);
            newPassword = null;
            selectedUserId = null;
            FacesMessageUtil.addInfoMessage("Password reset successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public void approve(Long userId) {
        try {
            userManagementService.approveMember(userId, getActingAdmin());
            search();
            FacesMessageUtil.addInfoMessage("Member approved and can now sign in");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public long getPendingApprovalCount() {
        return results == null ? 0 : results.stream().filter(UserAccountDto::isPendingApproval).count();
    }

    private UserAccount getActingAdmin() {
        if (!userSessionBean.isLoggedIn()) return null;
        return userAccountRepository.findById(userSessionBean.getLoggedInUser().getId()).orElse(null);
    }

    // Getters and Setters
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    public List<UserAccountDto> getResults() { return results; }
    public void setResults(List<UserAccountDto> results) { this.results = results; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public Long getSelectedUserId() { return selectedUserId; }
    public void setSelectedUserId(Long selectedUserId) { this.selectedUserId = selectedUserId; }
}
