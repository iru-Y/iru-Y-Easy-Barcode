import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ElementRef,
  OnDestroy,
} from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
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
import { AlertService } from '../../../domain/services/alert.services';
import { ActivatedRoute } from '@angular/router';
import { WebSocketService } from '../../../domain/services/web-socket.service';

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
  imports: [CommonModule, ReactiveFormsModule, FormsModule, NgOptimizedImage],
  templateUrl: './barcode-scanner.component.html',
  styleUrls: ['./barcode-scanner.component.css'],
})
export class BarcodeScannerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;

  countControl = new FormControl('', [Validators.required, Validators.min(1)]);
  newFilenameControl = new FormControl('', [Validators.required]);

  desiredCount: number | null = null;
  isSending = false;
  scanFeedback = '';

  private codeReader = new BrowserMultiFormatReader();
  stream: MediaStream | null = null;

  scannedBarcodes: string[] = [];
  scannedFiles: { filename: string }[] = [];
  selectedFilename: string | null = null;
  manualBarcode: string = '';

  showModal = false;

  batchModeEnabled = false;
  realtimeModeEnabled = false;

  private audioCtx!: AudioContext;

  constructor(
    private barcodeService: BarcodeService,
    private alertService: AlertService,
    private route: ActivatedRoute,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.audioCtx = new AudioContext();

    this.countControl.valueChanges.subscribe((value) => {
      const num = parseInt(value || '', 10);
      this.desiredCount = isNaN(num) || num < 1 ? null : num;
    });

    this.loadScannedFiles();

    this.route.queryParams.subscribe((params) => {
      this.batchModeEnabled =
        params['batch'] === 'true' || params['batch'] === true;
      this.realtimeModeEnabled =
        params['realtime'] === 'true' || params['realtime'] === true;

      if (this.realtimeModeEnabled) {
        this.connectWebSocket();
      }
    });
  }

  addManualBarcode(): void {
    const code = this.manualBarcode?.trim();
    if (!code) {
      this.alertService.show('Digite um código válido.');
      return;
    }
    if (this.scannedBarcodes.includes(code)) {
      this.alertService.show('Esse código já foi escaneado ou inserido.');
      return;
    }
    if (this.batchModeEnabled) this.scannedBarcodes.push(code);
    this.alertService.show(`Código manual adicionado: ${code}`);
    this.barcodeService.setLastScannedBarcode(code);
    this.manualBarcode = '';

    if (this.realtimeModeEnabled) {
      this.wsService.sendMessage(code);
    }
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
    if (this.batchModeEnabled) {
      this.startScanner();
    }
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

      const hints = new Map<any, any>();
      hints.set(DecodeHintType.TRY_HARDER, true);
      hints.set(DecodeHintType.POSSIBLE_FORMATS, [
        'EAN_13',
        'CODE_128',
        'QR_CODE',
        'UPC_A',
        'UPC_E',
        'ITF',
      ]);

      (this.codeReader as any).hints = hints;

      this.codeReader
        .decodeFromVideoDevice(
          null,
          this.videoElement.nativeElement,
          (result: Result | undefined, error: any) => {
            if (result) {
              this.onDetect(result);
            }
          }
        )
        .catch((err: any) => {
          console.error('Erro ao acessar a câmera:', err);
          this.alertService.show(
            'Erro ao acessar a câmera. Verifique as permissões.'
          );
        });
    } catch (err) {
      console.error('Erro ao iniciar o scanner:', err);
      this.alertService.show('Erro ao iniciar a câmera.');
    }
  }

  connectWebSocket() {
    this.wsService.connect('ws://localhost:8025/typer');
    this.wsService.messages$.subscribe((code) => {
      if (!this.scannedBarcodes.includes(code)) {
        this.scannedBarcodes.push(code);
        this.alertService.show(`Código recebido via WebSocket: ${code}`);
        this.barcodeService.setLastScannedBarcode(code);
      }
    });
  }

  playBeep() {
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }

    const oscillator = this.audioCtx.createOscillator();
    const gainNode = this.audioCtx.createGain();

    oscillator.type = 'square';
    oscillator.frequency.setValueAtTime(1000, this.audioCtx.currentTime);
    gainNode.gain.setValueAtTime(0.4, this.audioCtx.currentTime);

    oscillator.connect(gainNode);
    gainNode.connect(this.audioCtx.destination);

    oscillator.start();
    oscillator.stop(this.audioCtx.currentTime + 0.1); // 100ms beep
  }

  onDetect(result: Result): void {
    const raw = result.getText().trim();
    if (!raw) return;
    if (this.scannedBarcodes.includes(raw)) {
      this.scanFeedback = 'Código já escaneado. Tente outro.';
      return;
    }
    if (this.batchModeEnabled) this.scannedBarcodes.push(raw);

    this.scanFeedback = `Código escaneado: ${raw}`;

    this.playBeep();

    this.barcodeService.setLastScannedBarcode(raw);

    if (this.realtimeModeEnabled) {
      this.wsService.sendMessage(raw);
    }
  }

  openModal() {
    this.newFilenameControl.reset();
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  async confirmSend() {
    let filenameToSend = this.selectedFilename;
    if (!filenameToSend) {
      const input = this.newFilenameControl.value?.toString().trim();
      if (!input) {
        this.newFilenameControl.markAsTouched();
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
      this.closeModal();
    } catch (e) {
      console.error(e);
      this.alertService.show('Erro ao enviar os barcodes.');
    } finally {
      this.isSending = false;
    }
  }

  ngOnDestroy(): void {
    try {
      this.codeReader.reset();
    } catch {}
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
    }
    this.wsService.close();
  }
}
