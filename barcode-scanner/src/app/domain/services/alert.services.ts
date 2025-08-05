import { Injectable, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private platformId = inject(PLATFORM_ID);

  show(message: string): void {
    if (isPlatformBrowser(this.platformId)) {
      alert(message);
    } else {
      console.log('🔔 SSR: Alerta ignorado ->', message);
    }
  }
}
