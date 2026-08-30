package com.cloud.storage.service.user;

import com.cloud.storage.entity.User;


public interface UserService {

    User findByUsername(String username);
}
