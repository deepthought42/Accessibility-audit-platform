import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-integrations-panel',
  templateUrl: './integrations-panel.component.html',
  styleUrl: './integrations-panel.component.css'
})
export class IntegrationsPanelComponent {
  auth = inject(AuthService);

  constructor(){ 
    // Component initialization
  }
  
  selectIntegration(): void {
    // Integration selection logic can be added here if needed
  }
}
