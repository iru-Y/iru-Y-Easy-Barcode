import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LastScanComponent } from './last-scan.component';

describe('LastScanComponent', () => {
  let component: LastScanComponent;
  let fixture: ComponentFixture<LastScanComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LastScanComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LastScanComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
