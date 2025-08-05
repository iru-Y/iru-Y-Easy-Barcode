import { Component, OnInit } from '@angular/core';
import { BarcodeService } from '../domain/services/barcode.service';
import { ScannerFileDto } from '../domain/dtos/scanner-file.dto';
import { MatIcon } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { FooterNavComponent } from "../footer-nav/footer-nav.component";
import { MatButtonModule } from '@angular/material/button';
@Component({
  selector: 'app-scan-list',
  standalone: true,
  imports: [MatIcon, CommonModule, FooterNavComponent, MatButtonModule],
  templateUrl: './scan-list.component.html',
  styleUrls: ['./scan-list.component.css'],
})
export class ScanListComponent implements OnInit {
  barcodes: ScannerFileDto[] = [];
  selectedDate: Date | null = null;
  code: string[] = [];
  modalOpen = false;
  scannedFiles: ScannerFileDto[] = [];

  constructor(private barcodeService: BarcodeService) {}

  async ngOnInit() {
  this.barcodeService.getBarcodes().subscribe((data) => {
    this.barcodes = data;
  });

  if (this.barcodes.length === 0) {
    await this.barcodeService.getUploadedBarcodes();
  }
}


  openModal(item: ScannerFileDto) {
    this.code = item.barcodes ?? [];
    this.selectedDate = new Date(item.createdAt);
    this.modalOpen = true;
  }

  closeModal() {
    this.modalOpen = false;
    this.code = [];
  }

  async deleteBarcode(filename: string) {
  if (!confirm(`Tem certeza que deseja deletar "${filename}"?`)) return;

  try {
    await this.barcodeService.deleteBarcodeByFilename(filename);
    console.log(`🗑️ Deletado: ${filename}`);
  } catch (err) {
    console.error('Erro ao deletar arquivo:', err);
    alert('Falha ao deletar o arquivo.');
  }
}


  copyToClipboard(text: string) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        console.log('Copiado:', text);
      }).catch(err => {
        console.error('Erro ao copiar:', err);
      });
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed'; 
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      try {
        document.execCommand('copy');
        console.log('Copiado com fallback:', text);
      } catch (err) {
        console.error('Erro no fallback:', err);
      }
      document.body.removeChild(textarea);
    }
  }
}
