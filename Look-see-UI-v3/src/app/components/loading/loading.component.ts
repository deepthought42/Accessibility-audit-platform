import { Component, OnInit, inject } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { ProgressBarMode } from '@angular/material/progress-bar';
import { AuthService } from '@auth0/auth0-angular';
import { faFrown, faMeh, faSmileBeam } from '@fortawesome/free-solid-svg-icons';
import { AuditService } from '../../services/audit.service';

@Component({
  selector: 'app-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.scss']
})
export class LoadingComponent implements OnInit {
  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  
  //loading bar config
  color: ThemePalette = 'primary'
  accent_color: ThemePalette = 'accent'
  mode: ProgressBarMode = 'indeterminate'
  value = 50
  diameter = 50

  audit_record_id  = -1
  create_account_message = "Create Account"
  
  //interval tracking value
  carousel_interval_id: any | undefined

  visible_slide = 'carousel-1'
 
  audit_service = inject(AuditService);
  auth = inject(AuthService);

  ngOnInit(): void {
    this.visible_slide = 'carousel-1'
    this.rotateCarouselInterval()
  }


  rotateCarouselInterval(): void {
     //get audit stats for audit record
    this.carousel_interval_id =  setInterval(()=>{  
      if(this.visible_slide == 'carousel-1'){
        this.visible_slide = 'carousel-2'
      }
      else if(this.visible_slide == 'carousel-2'){
        this.visible_slide = 'carousel-3'
      }
      else if(this.visible_slide == 'carousel-3'){
        this.visible_slide = 'carousel-4'
      }
      else if(this.visible_slide == 'carousel-4'){
        this.visible_slide = 'carousel-5'
      }
      else if(this.visible_slide == 'carousel-5'){
        this.visible_slide = 'carousel-1'
      }
    }, 13000)
  }

  selectSlide(slide_name: string):void {
    this.visible_slide = slide_name
  }
}
