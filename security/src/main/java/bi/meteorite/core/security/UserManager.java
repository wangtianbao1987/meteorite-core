/*
 * Copyright 2015 OSBI Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bi.meteorite.core.security;

import bi.meteorite.core.api.objects.MeteoriteUser;
import bi.meteorite.core.api.security.IUserManagement;
import bi.meteorite.core.api.security.MeteoriteSecurityException;

import java.util.List;

/**
 * Created by bugg on 03/07/15.
 */
public class UserManager implements IUserManagement {
  @Override
  public MeteoriteUser addUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public boolean deleteUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return false;
  }

  @Override
  public MeteoriteUser setUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public MeteoriteUser getUser(int id) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public String[] getRoles(MeteoriteUser u) throws MeteoriteSecurityException {
    return new String[0];
  }

  @Override
  public void addRole(MeteoriteUser u) throws MeteoriteSecurityException {

  }

  @Override
  public void removeRole(MeteoriteUser u) throws MeteoriteSecurityException {

  }

  @Override
  public void removeUser(String username) throws MeteoriteSecurityException {

  }

  @Override
  public MeteoriteUser updateUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public boolean isAdmin() throws MeteoriteSecurityException {
    return false;
  }

  @Override
  public List<String> getAdminRoles() throws MeteoriteSecurityException {
    return null;
  }
}