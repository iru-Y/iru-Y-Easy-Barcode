import { Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { ScannerFileDto } from '../dtos/scanner-file.dto';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class BarcodeService {
  private url = `${environment.apiUrl}/barcode`;

  private barcodesSubject = new BehaviorSubject<ScannerFileDto[]>([]);
  barcodes$ = this.barcodesSubject.asObservable();

  private lastScannedBarcodeSubject = new BehaviorSubject<String | null>(null);
  lastScannedBarcode$ = this.lastScannedBarcodeSubject.asObservable();

  constructor(private http: HttpClient) {}


  setLastScannedBarcode(barcode: string) {
  this.lastScannedBarcodeSubject.next(barcode);
  setTimeout(() => this.lastScannedBarcodeSubject.next(null), 5000);
}

  async sendBarcode(
    filename: string,
    barcodes: string[]
  ): Promise<ScannerFileDto> {
    const body = { filename, barcodes };
    const result = await firstValueFrom(
      this.http.post<ScannerFileDto>(`${this.url}`, body, {
        headers: { 'Content-Type': 'application/json' },
      })
    );

    const current = this.barcodesSubject.value;
    this.barcodesSubject.next([...current, result]);

    return result;
  }

  async getUploadedBarcodes(): Promise<ScannerFileDto[]> {
    const result = await firstValueFrom(
      this.http.get<ScannerFileDto[]>(`${this.url}`)
    );

    this.barcodesSubject.next(result);
    return result;
  }

  getBarcodes(): Observable<ScannerFileDto[]> {
    return this.barcodes$;
  }

  setBarcodes(barcodes: ScannerFileDto[]) {
    this.barcodesSubject.next(barcodes);
  }

  async deleteBarcodeByFilename(filename: string): Promise<void> {
  await firstValueFrom(this.http.delete(`${this.url}/${filename}`));

  const updated = this.barcodesSubject.value.filter(b => b.filename !== filename);
  this.barcodesSubject.next(updated);
}

}
