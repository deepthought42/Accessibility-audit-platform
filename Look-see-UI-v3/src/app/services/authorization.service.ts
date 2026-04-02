import { Injectable, inject } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { AuthService } from '@auth0/auth0-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {
  private auth = inject(AuthService);
  private jwtHelperService = inject(JwtHelperService);

  isAuthorized(allowed_role: string, token: string): boolean {
    // check if the list of allowed roles is empty, if empty, authorize the user to access the page
    
    if (allowed_role == null || allowed_role.length === 0) {
      return true;
    }

    // get token from local storage or state management
      // decode token to read the payload details
    const decodeToken = this.jwtHelperService.decodeToken(token);
  
    // check if it was decoded successfully, if not the token is not valid, deny access
    if (!decodeToken) {
      console.log('Invalid token');
      return false;
    }
    const permissions = Array.isArray(decodeToken['permissions']) ? decodeToken['permissions'] : [];

    // check if the user roles is in the list of allowed roles, return true if allowed and false if not allowed
    return permissions.length == 0 || permissions.includes(allowed_role);

  }
}
