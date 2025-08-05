export interface TokenResponseDTO {
  accessToken: string;
  refreshToken: string | null; // será null no refresh
  tokenType: string;
}
