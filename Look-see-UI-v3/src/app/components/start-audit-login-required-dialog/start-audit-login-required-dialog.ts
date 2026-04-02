import { Component, EventEmitter, inject, Output } from "@angular/core"
import { FormControl } from "@angular/forms"
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog"

@Component({
  selector: 'app-start-audit-login-required-dialog',
    templateUrl: './start-audit-login-required-dialog.html'
  })
  export class StartSinglePageAuditLoginRequired {
  
    error = ""
    isLoading = false
    recommendation_str = new FormControl('')
    description = new FormControl('')
    issue_type = new FormControl('')
    priority = new FormControl('')
    url = ''
    create_account_message = "Create Account"

  
    @Output() clear_audit_event = new EventEmitter<boolean>();

    dialogRef = inject(MatDialogRef<StartSinglePageAuditLoginRequired>);
    data = inject(MAT_DIALOG_DATA) as Record<string, unknown>;

    constructor() {
        this.url = this.data['url'] as string;
    }
    
    onNoClick(): void {
      this.dialogRef.close();
    }
  
    
    startSinglePageAudit(): void {
        /** might need for quick audit and audit review */
        /*
        this.auditor_service.startIndividualAudit(this.url)
        .pipe(
          catchError(err => {
            this.error = "uh oh...it looks like our servers decided to take a coffee break. Please wait a minute then try again.";
            this.isLoading = false
            return throwError(err);
        })
        ).subscribe(
          (data) => {
            //this.show_intro_panel = true
            sessionStorage.setItem("audit_record_id", `${data.id}`)
            sessionStorage.setItem('page',  JSON.stringify(data.simplePage) )

            this.router.navigateByUrl('/').then(e => {
              if (e) {
                //console.log("Navigation is successful!");
              } else {
                this.isLoading = false
                console.log("Oh no! The page seems to have gone on a journey. We'll have a look-see and bring it back as soon as possible. Please wait a minute and try again.");
              }
            })
            
          }
          );
        */
        this.clear_audit_event.emit(true);
        sessionStorage.removeItem("audit_record_id");
        sessionStorage.removeItem("audit_record_id")
        this.dialogRef.close({ is_audit_started: true });

    }
  }