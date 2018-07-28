import { Services } from './core/Services';
import { autoinject } from "aurelia-framework";
import { Router } from "aurelia-router";

@autoinject
export class Create {

  isProcessing = false;
  learnerTypes: string[] = ["AdaptivePLT", "PLTEnsembleBoosted", "PLTEnsembleBoostedWithThreshold"];//, "PLTAdaptiveEnsemble"

  //Selected learner type. This empty initialization is needed for correct functionality.
  private _selectedLearnerType: string = "";

  learnerConfig: any;
  errorMessage: string;

  constructor(private services: Services, private router: Router) {
    this.learnerConfig = {};
  }

  get trash() {
    return JSON.stringify(this.learnerConfig);
  }

  get selectedLearnerType() {
    return this._selectedLearnerType;
  }

  set selectedLearnerType(val: string) {
    this._selectedLearnerType = val;
    this.errorMessage = "";
  }

  async createLearner() {
    const self = this;
    self.errorMessage = "";
    self.isProcessing = true;

    await self.services
      .post(`${self.services.endpoints.learner}/create`, {
        learnerClass: self.selectedLearnerType,
        configJson: JSON.stringify(self.learnerConfig)
      })
      .then((id: any) => {
        this.router.navigateToRoute("trainAndTest", { learnerId: id });
      })
      .catch((error) => {
        console.log(error);
        self.errorMessage = "Something went wrong; could not create the learner.";
      })
      .then(() => self.isProcessing = false);
  }
}
