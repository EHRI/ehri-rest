package eu.ehri.project.views.api;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;

import java.util.List;

public interface UserProfilesApi {
    void addWatching(UserProfile user, List<String> ids) throws ItemNotFound;

    void removeWatching(UserProfile user, List<String> ids) throws ItemNotFound;

    void addFollowers(UserProfile user, List<String> ids) throws ItemNotFound;

    void removeFollowers(UserProfile user, List<String> ids) throws ItemNotFound;

    void addBlocked(UserProfile user, List<String> ids) throws ItemNotFound;

    void removeBlocked(UserProfile user, List<String> ids) throws ItemNotFound;
}
