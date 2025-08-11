import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { FooterNavComponent } from "../footer-nav/footer-nav.component";
import { UserService } from '../domain/services/user';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgOptimizedImage, FooterNavComponent],
  templateUrl: './user.html',
  styleUrls: ['./user.css']
})
export class UserComponent {
  passwordForm: FormGroup;
  showPassword = false;

  constructor(private fb: FormBuilder, private userService: UserService) {
    this.passwordForm = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: ['', Validators.required],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { passwordsMismatch: true };
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) return;

    const { currentPassword, newPassword } = this.passwordForm.value;

    this.userService.changePassword({
      oldPassword: currentPassword,
      newPassword: newPassword
    }).subscribe({
      next: () => {
        alert('Senha atualizada com sucesso!');
        this.passwordForm.reset();
      },
      error: err => {
        alert(err.error || 'Erro ao atualizar senha');
      }
    });
  }
}
