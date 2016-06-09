package eu.ehri.project.api;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;

import java.util.List;

public interface UserProfilesApi {
    UserProfile addWatching(String userId, List<String> ids) throws ItemNotFound;

    UserProfile removeWatching(String userId, List<String> ids) throws ItemNotFound;

    UserProfile addFollowers(String userId, List<String> ids) throws ItemNotFound;

    UserProfile removeFollowers(String userId, List<String> ids) throws ItemNotFound;

    UserProfile addBlocked(String userId, List<String> ids) throws ItemNotFound;

    UserProfile removeBlocked(String userId, List<String> ids) throws ItemNotFound;
}
