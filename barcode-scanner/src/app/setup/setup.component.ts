import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FooterNavComponent } from "../footer-nav/footer-nav.component";

@Component({
  selector: 'app-setup',
  templateUrl: './setup.component.html',
  styleUrls: ['./setup.component.css'],
  imports: [FooterNavComponent]
})
export class SetupComponent {
  batchMode = false;
  realtimeMode = false;

  constructor(private router: Router) {}

  onBatchChange(event: Event) {
    this.batchMode = (event.target as HTMLInputElement).checked;
  }

  onRealtimeChange(event: Event) {
    this.realtimeMode = (event.target as HTMLInputElement).checked;
  }

  onConfirm() {
    if (!this.batchMode && !this.realtimeMode) {
      alert('Selecione pelo menos uma opção.');
      return;
    }

    this.router.navigate(['/home'], {
      queryParams: {
        batch: this.batchMode,
        realtime: this.realtimeMode
      }
    });
  }
}
