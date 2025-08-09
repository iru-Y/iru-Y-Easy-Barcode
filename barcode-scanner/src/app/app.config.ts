import { CommonModule } from '@angular/common';
import {
  provideHttpClient,
  withFetch,
  withInterceptors,
} from '@angular/common/http';
import {
  ApplicationConfig,
  provideZoneChangeDetection,
  importProvidersFrom,
} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import {
  provideClientHydration,
  withEventReplay,
} from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './domain/interceptor/auth.interceptor';
import { tokenRefreshInterceptor } from './domain/interceptor/token-refresh.interceptor';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { ScanListComponent } from './scan-list/scan-list.component';
import { provideAnimations } from '@angular/platform-browser/animations';
import { SetupComponent } from './setup/setup.component';
import { BarcodeScannerComponent } from './home/components/barcode-scanner/barcode-scanner.component';
export const routes = [
  { path: '', component: LoginComponent },
  { path: 'home', component: HomeComponent },
  { path: 'files', component: ScanListComponent }, // temporário
  { path: 'setup', component: SetupComponent },
  { path: 'profile', component: LoginComponent },
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideClientHydration(withEventReplay()),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor, tokenRefreshInterceptor])
    ),
    provideAnimations(),
    importProvidersFrom(CommonModule, ReactiveFormsModule),
  ],
};

