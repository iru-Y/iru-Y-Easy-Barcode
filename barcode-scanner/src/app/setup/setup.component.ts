import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FooterNavComponent } from "../footer-nav/footer-nav.component";

@Component({
  selector: 'app-setup',
  templateUrl: './setup.component.html',
  styleUrls: ['./setup.component.css'],
  imports: [FooterNavComponent, FormsModule]
})
export class SetupComponent implements OnInit {
  batchMode = false;
  realtimeMode = false;

  constructor(private router: Router) {}

  ngOnInit() {
    const savedBatch = localStorage.getItem('batchMode');
    const savedRealtime = localStorage.getItem('realtimeMode');

    if (savedBatch !== null) this.batchMode = savedBatch === 'true';
    if (savedRealtime !== null) this.realtimeMode = savedRealtime === 'true';
  }

  onBatchChange() {
    localStorage.setItem('batchMode', String(this.batchMode));
    this.onModeChange();
  }

  onRealtimeChange() {
    localStorage.setItem('realtimeMode', String(this.realtimeMode));
    this.onModeChange();
  }

  onModeChange() {
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
