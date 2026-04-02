import { Component, inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AuditService } from "../../services/audit.service";

@Component({
  selector: 'app-element-info-dialog',
    templateUrl: './element-info-dialog.html'
  })
  export class ElementInfoDialog {
  
  auditService = inject(AuditService);
  dialogRef = inject(MatDialogRef<ElementInfoDialog>);
  element = inject(MAT_DIALOG_DATA) as Record<string, unknown>;
    
  onNoClick(): void {
    this.dialogRef.close()
  }

  printObject(val: unknown): string {
    return JSON.stringify(val)
  }

}