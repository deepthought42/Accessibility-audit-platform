import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';

interface OnboardingStep {
  id: string;
  title: string;
  description: string;
  icon: string;
  status: 'pending' | 'active' | 'complete';
}

interface FeaturePreview {
  title: string;
  description: string;
  icon: string;
}

@Component({
  selector: 'app-audit-onboarding',
  templateUrl: './audit-onboarding.component.html',
  styleUrls: ['./audit-onboarding.component.scss']
})
export class AuditOnboardingComponent implements OnInit, OnDestroy, OnChanges {
  @Input() url = '';
  @Input() auditStarted = false;
  @Input() auditComplete = false;
  @Output() viewResults = new EventEmitter<void>();

  steps: OnboardingStep[] = [
    {
      id: 'capture',
      title: 'Capturing Page',
      description: 'Taking a screenshot and extracting DOM elements from your page',
      icon: 'camera',
      status: 'pending'
    },
    {
      id: 'content',
      title: 'Content Analysis',
      description: 'Evaluating readability, alt text, and written content quality',
      icon: 'document',
      status: 'pending'
    },
    {
      id: 'accessibility',
      title: 'Accessibility Check',
      description: 'Testing WCAG compliance, color contrast, ARIA labels, and semantic HTML',
      icon: 'accessibility',
      status: 'pending'
    },
    {
      id: 'visual',
      title: 'Visual Design Review',
      description: 'Analyzing typography, whitespace, branding, and color palette',
      icon: 'palette',
      status: 'pending'
    },
    {
      id: 'architecture',
      title: 'Information Architecture',
      description: 'Checking SEO, metadata, link structure, and navigation',
      icon: 'sitemap',
      status: 'pending'
    }
  ];

  featurePreviews: FeaturePreview[] = [
    {
      title: 'Visual Issue Map',
      description: 'See accessibility issues pinpointed directly on a screenshot of your page. Each issue is marked exactly where it occurs.',
      icon: 'map'
    },
    {
      title: 'Non-Visual Issue Report',
      description: 'Metadata problems, SEO issues, and other non-rendered HTML problems are grouped in a dedicated panel.',
      icon: 'code'
    },
    {
      title: 'Fix Suggestions',
      description: 'Every issue comes with the WCAG success criterion it fails and a code-level recommendation — not just a red flag.',
      icon: 'sparkle'
    },
    {
      title: 'Exportable PDF Report',
      description: 'Download a comprehensive PDF report with all findings, scores, and recommendations to share with your team.',
      icon: 'download'
    }
  ];

  currentFeatureIndex = 0;
  stepTimerIds: ReturnType<typeof setTimeout>[] = [];
  featureRotationId: ReturnType<typeof setInterval> | undefined;

  ngOnInit(): void {
    if (this.auditStarted) {
      this.startProgressSimulation();
      this.startFeatureRotation();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['auditStarted'] && this.auditStarted) {
      this.startProgressSimulation();
      this.startFeatureRotation();
    }
    if (changes['auditComplete'] && this.auditComplete) {
      this.completeAllSteps();
    }
  }

  ngOnDestroy(): void {
    this.stepTimerIds.forEach(id => clearTimeout(id));
    if (this.featureRotationId) {
      clearInterval(this.featureRotationId);
    }
  }

  startProgressSimulation(): void {
    // Simulate step progression with realistic timing
    const delays = [0, 3000, 7000, 12000, 18000];

    this.steps.forEach((step, index) => {
      // Mark as active
      const activeTimer = setTimeout(() => {
        step.status = 'active';
      }, delays[index]);
      this.stepTimerIds.push(activeTimer);

      // Mark as complete (except last step - that completes when audit finishes)
      if (index < this.steps.length - 1) {
        const completeTimer = setTimeout(() => {
          step.status = 'complete';
        }, delays[index + 1]);
        this.stepTimerIds.push(completeTimer);
      }
    });
  }

  startFeatureRotation(): void {
    this.featureRotationId = setInterval(() => {
      this.currentFeatureIndex = (this.currentFeatureIndex + 1) % this.featurePreviews.length;
    }, 5000);
  }

  completeAllSteps(): void {
    this.steps.forEach(step => step.status = 'complete');
  }

  onViewResults(): void {
    this.viewResults.emit();
  }

  getStepIcon(step: OnboardingStep): string {
    switch (step.icon) {
      case 'camera': return 'M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z M15 13a3 3 0 11-6 0 3 3 0 016 0z';
      case 'document': return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      case 'accessibility': return 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z';
      case 'palette': return 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01';
      case 'sitemap': return 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z';
      default: return '';
    }
  }

  getFeatureIcon(preview: FeaturePreview): string {
    switch (preview.icon) {
      case 'map': return 'M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7';
      case 'code': return 'M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4';
      case 'sparkle': return 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z';
      case 'download': return 'M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      default: return '';
    }
  }
}
