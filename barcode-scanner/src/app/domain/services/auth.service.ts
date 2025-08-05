import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TokenResponseDTO } from '../dtos/token-response.dto';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(credentials: { email: string; password: string }): Observable<TokenResponseDTO> {
    return this.http.post<TokenResponseDTO>(
      `${environment.apiUrl}/auth/login`,
      credentials,
      { withCredentials: true } // permite o navegador guardar o cookie (refresh token)
    );
  }
}
