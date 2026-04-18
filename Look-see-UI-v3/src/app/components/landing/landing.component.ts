import { Component, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { AuditorService } from '../../services/auditor.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { normaliseUrl } from '../../utils/url';

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
    const rawUrl = this.form.value.url || '';
    const normalised = normaliseUrl(rawUrl);

    if (!normalised.ok) {
      this.error = normalised.error;
      return;
    }

    const url = normalised.url;
    this.error = '';
    this.isLoading = true;
    this.segmentio.sendUxAuditStartedMessage(url);

    this.auditorService.startAudit(url, 'PAGE').subscribe(
      (data) => {
        this.isLoading = false;
        if (data && data.id) {
          sessionStorage.setItem('audit_record_id', data.id.toString());
          sessionStorage.setItem('is_audit_started', 'true');
          sessionStorage.setItem('url', url.replace(/^https?:\/\//, ''));
          this.router.navigate(['/audit', data.id, 'review']);
        }
      },
      () => {
        this.error = "We couldn't reach that URL. Check the address and try again.";
        this.isLoading = false;
      }
    );
  }

  /**
   * @deprecated kept as a compatibility shim; call `normaliseUrl()` directly.
   * Remove once no callers remain (no production callers remain in this file).
   */
  isValidURL(domain_url: string): boolean {
    return normaliseUrl(domain_url).ok;
  }
}
