import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BarcodeService } from '../../../domain/services/barcode.service';

@Component({
  selector: 'app-last-scan',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './last-scan.component.html',
  styleUrls: ['./last-scan.component.css'],
})
export class LastScanComponent implements OnInit {
  lastBarcode: String | null = null;

  constructor(private barcodeService: BarcodeService) {}

  ngOnInit(): void {
    this.barcodeService.lastScannedBarcode$.subscribe((barcode) => {
      this.lastBarcode = barcode;
    });
  }
}
