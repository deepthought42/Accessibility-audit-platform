import { Component, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { AuditorService } from '../../services/auditor.service';
import { SegmentIOService } from '../../services/segmentio.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  isLoading = false;
  error = '';

  form = new FormGroup({
    url: new FormControl(''),
  });

  router = inject(Router);
  auth = inject(AuthService);
  auditorService = inject(AuditorService);
  segmentio = inject(SegmentIOService);

  startAudit(): void {
    const url = this.form.value.url?.trim() || '';

    if (!url.length) {
      this.error = 'Please enter a URL to audit.';
      return;
    }

    if (!this.isValidURL(url)) {
      this.error = 'Please enter a valid URL (e.g. https://www.example.com)';
      return;
    }

    this.error = '';
    this.isLoading = true;
    this.segmentio.sendDomainUxAuditStartedMessage(url);

    this.auditorService.startAudit(url, 'PAGE').subscribe(
      (data) => {
        this.isLoading = false;
        if (data && data.id) {
          sessionStorage.setItem('audit_record_id', data.id.toString());
          sessionStorage.setItem('is_audit_started', 'true');
          sessionStorage.setItem('url', url.replace('https://', '').replace('http://', ''));
          this.router.navigate(['/audit', data.id, 'review']);
        }
      },
      () => {
        this.error = 'Something went wrong starting the audit. Please try again.';
        this.isLoading = false;
      }
    );
  }

  isValidURL(domain_url: string): boolean {
    const urlregex = /^((https?|ftp):\/\/)?([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|app|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|website|space|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
    return urlregex.test(domain_url);
  }
}
