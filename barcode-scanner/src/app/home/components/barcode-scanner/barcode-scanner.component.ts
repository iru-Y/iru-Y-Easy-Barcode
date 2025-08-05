import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ElementRef,
  TemplateRef,
  OnDestroy,
} from '@angular/core';
import {
  FormControl,
  Validators,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import {
  BrowserMultiFormatReader,
  Result,
  DecodeHintType,
} from '@zxing/library';
import { BarcodeService } from '../../../domain/services/barcode.service';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { AlertService } from '../../../domain/services/alert.services';
import {
  MatDialog,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';

interface ExtendedMediaTrackConstraintSet extends MediaTrackConstraintSet {
  focusMode?: 'none' | 'manual' | 'single-shot' | 'continuous';
  torch?: boolean;
}

interface ExtendedMediaTrackCapabilities extends MediaTrackCapabilities {
  torch?: boolean;
}

@Component({
  selector: 'app-barcode-scanner',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatRadioModule,
  ],
  templateUrl: './barcode-scanner.component.html',
  styleUrls: ['./barcode-scanner.component.css'],
})
export class BarcodeScannerComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('modalTemplate') modalTemplate!: TemplateRef<any>;
  dialogRef!: MatDialogRef<any>;
  countControl = new FormControl('', [Validators.required, Validators.min(1)]);
  desiredCount: number | null = null;
  isSending = false;
  isTorchOn = false;
  scanFeedback = '';

  private codeReader = new BrowserMultiFormatReader();
  private stream: MediaStream | null = null;
  scannedBarcodes: string[] = [];
  showModal = false;
  scannedFiles: { filename: string }[] = [];
  selectedFilename: string | null = null;
  newFilenameControl = new FormControl('', [Validators.required]);
  manualBarcode: string = '';

  constructor(
    private barcodeService: BarcodeService,
    private alertService: AlertService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.countControl.valueChanges.subscribe((value) => {
      const num = parseInt(value || '', 10);
      this.desiredCount = isNaN(num) || num < 1 ? null : num;
    });

    this.loadScannedFiles();
  }

  addManualBarcode(): void {
  const code = this.manualBarcode.trim();

  if (!code) {
    this.alertService.show('Digite um código válido.');
    return;
  }

  if (this.scannedBarcodes.includes(code)) {
    this.alertService.show('Esse código já foi escaneado ou inserido.');
    return;
  }

  this.scannedBarcodes.push(code);
  this.alertService.show(`Código manual adicionado: ${code}`);
  this.barcodeService.setLastScannedBarcode(code);

  this.manualBarcode = '';
}


  async loadScannedFiles() {
    try {
      this.scannedFiles = await this.barcodeService.getUploadedBarcodes();
    } catch {
      this.scannedFiles = [];
      this.alertService.show('Erro ao carregar arquivos escaneados.');
    }
  }

  ngAfterViewInit(): void {
    this.startScanner();
  }

  async startScanner(): Promise<void> {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      this.alertService.show('Seu navegador não suporta acesso à câmera.');
      return;
    }

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'environment',
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      });

      this.videoElement.nativeElement.srcObject = this.stream;

      const hints = new Map();
      hints.set(DecodeHintType.TRY_HARDER, true);
      hints.set(DecodeHintType.POSSIBLE_FORMATS, [
        'EAN_13',
        'CODE_128',
        'QR_CODE',
        'UPC_A',
        'UPC_E',
        'ITF',
      ]);

      this.codeReader.hints = hints;

      this.codeReader
        .decodeFromVideoDevice(
          null,
          this.videoElement.nativeElement,
          (result, error) => {
            if (result) {
              this.onDetect(result);
            } else if (error) {
              this.handleScanError(error);
            }
          }
        )
        .catch((err) => {
          console.error('Erro ao acessar a câmera:', err);
          this.alertService.show(
            'Erro ao acessar a câmera. Verifique as permissões.'
          );
        });

      this.enableAutoFocus();
    } catch (err) {
      console.error('Erro ao iniciar o scanner:', err);
      this.alertService.show('Erro ao iniciar a câmera.');
    }
  }

  handleScanError(error: any): void {
    if (error?.message?.includes('Failed to decode')) {
      this.scanFeedback =
        'Código de barras difícil de ler. Ajuste a distância ou iluminação.';
    } else {
      this.scanFeedback = 'Aguardando leitura de código de barras...';
    }
  }

  onDetect(result: Result): void {
    const raw = result.getText().trim();
    if (!raw) return;

    if (this.scannedBarcodes.includes(raw)) {
      this.scanFeedback = 'Código já escaneado. Tente outro.';
      return;
    }

    this.scannedBarcodes.push(raw);
    this.scanFeedback = `Código escaneado: ${raw}`;
    this.alertService.show(`Código escaneado: ${raw}`);
    this.barcodeService.setLastScannedBarcode(raw);
  }

  async toggleTorch(): Promise<void> {
    if (!this.stream) return;

    const track = this.stream.getVideoTracks()[0];
    const capabilities =
      track.getCapabilities() as ExtendedMediaTrackCapabilities;

    if (!capabilities.torch) {
      this.alertService.show('Este dispositivo não suporta lanterna.');
      return;
    }

    try {
      await track.applyConstraints({
        advanced: [
          { torch: !this.isTorchOn } as ExtendedMediaTrackConstraintSet,
        ],
      });
      this.isTorchOn = !this.isTorchOn;
      this.alertService.show(
        `Lanterna ${this.isTorchOn ? 'ativada' : 'desativada'}.`
      );
    } catch (err) {
      console.error('Erro ao alternar lanterna:', err);
      this.alertService.show('Erro ao alternar lanterna.');
    }
  }

  enableAutoFocus(): void {
    if (!this.stream) return;

    const track = this.stream.getVideoTracks()[0];
    const capabilities =
      track.getCapabilities() as ExtendedMediaTrackCapabilities;

    if ('focusMode' in capabilities) {
      track
        .applyConstraints({
          advanced: [
            { focusMode: 'continuous' } as ExtendedMediaTrackConstraintSet,
          ],
        })
        .catch((err) => {
          console.warn('Autofocus não suportado:', err);
        });
    } else {
      console.warn('focusMode não suportado neste dispositivo.');
    }
  }

  openModal() {
    this.dialogRef = this.dialog.open(this.modalTemplate, {
      width: '400px',
      disableClose: true,
      panelClass: 'custom-dialog-container',
    });
  }

  async confirmSend() {
    if (!this.dialogRef) return;

    let filenameToSend = this.selectedFilename;
    if (!filenameToSend) {
      const input = this.newFilenameControl.value?.trim();
      if (!input) {
        this.alertService.show('Informe um nome válido para o novo arquivo.');
        return;
      }
      filenameToSend = input.replace(/\.csv$/i, '');
    }

    this.isSending = true;

    const quantity = this.desiredCount ?? 1;
    const expandedBarcodes = this.scannedBarcodes.flatMap((code) =>
      Array(quantity).fill(code)
    );

    try {
      await this.barcodeService.sendBarcode(filenameToSend, expandedBarcodes);
      this.alertService.show(
        `📦 Barcodes enviados para arquivo: ${filenameToSend}`
      );
      this.scannedBarcodes = [];
      this.countControl.setValue('');
      this.countControl.markAsUntouched();
      await this.loadScannedFiles();
      this.dialogRef.close();
    } catch (e) {
      this.alertService.show('Erro ao enviar os barcodes.');
    }

    this.isSending = false;
  }

  ngOnDestroy(): void {
    this.codeReader.reset();
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
    }
  }
}
