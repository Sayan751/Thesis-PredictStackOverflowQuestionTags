import { Predictions } from './core/models/Predictions';
import { LearnerInfo, CumulativeFmeasures } from './core/models/LearnerInfo';
import { Services } from './core/Services';
import { inject, NewInstance } from "aurelia-framework";
import { ValidationController, ValidationRules } from "aurelia-validation";
import { EventAggregator } from 'aurelia-event-aggregator';
import { DialogService } from 'aurelia-dialog';
import { FmChart } from "./fm-chart"
import { Utility } from "./core/Utility";
// import promisify from "es6-promisify";
const promisify = require("es6-promisify");
import "jquery";
import "bootstrap"

@inject(Services, NewInstance.of(ValidationController), EventAggregator, DialogService)
export class TrainAndTest {

  learnerId: string;
  learnerInfo: LearnerInfo | undefined;
  validationResults: any[] = [];
  isProcessing = false;
  current: number = 0;
  total: number = 0;
  question: any;
  predictions: Predictions;
  activateRandomQuestionInput: boolean = false;
  qIpBtn: any;
  message: string | undefined;

  constructor(private services: Services, private validationController: ValidationController, private eventAggregator: EventAggregator, private dialogService: DialogService) {

    this.validationController.addObject(this,
      ValidationRules
        .ensure((vm: TrainAndTest) => vm.learnerId)
        .displayName("Learner Id")
        .required()
        .matches(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i).withMessage("Learner Id needs to be UUID") //[1-5][0-9a-f]{3}
        .rules);

    this.eventAggregator.subscribe('QuestionInput:QuestionCreated', () => {
      this.randomQuestionInputActivationHandler();
    });
  }

  activate(params: any) {
    if (params.learnerId) {
      this.learnerId = params.learnerId;
      this.fetchLearner();
    }
  }

  randomQuestionInputActivationHandler() {
    this.activateRandomQuestionInput = !this.activateRandomQuestionInput;
    if (this.activateRandomQuestionInput)
      this.question = undefined;
    $(this.qIpBtn).button('toggle');
  }

  async fetchLearner() {
    const self = this;
    let isValid = false;
    await self.validationController
      .validate()
      .then(vr => {
        // self.validationResults = vr.results;
        isValid = vr.valid;
      });

    if (isValid) {
      self.isProcessing = true;
      self.learnerInfo = undefined;
      self.question = undefined;
      self.validationResults = [];
      self.services
        .get(`${self.services.endpoints.learner}/${self.learnerId}`)
        .then((data: any) => {
          self.learnerInfo = Object.assign(new LearnerInfo(), data);
        })
        .catch(error => {
          console.log(error);
          self.validationResults.push({ message: "Learner not found." });
        }).then(() => {
          self.isProcessing = false;
        });
    }
  }

  async pickQuestion() {
    const self = this;
    self.isProcessing = true;
    await self.services
      .get(`${self.services.endpoints.learner}/${self.learnerId}/nextQuestion`)
      .then((data: any) => {
        self.question = data;
      }).then(() => {
        self.isProcessing = false;
      });
  }

  async train(n?: number): Promise<void> {

    this.validationResults = [];
    this.isProcessing = true;

    if (n != undefined)
      await this.trainN(n);
    else
      await this.trainOnCurrentQuestion();

    this.isProcessing = false;
    this.current = 0;
  }

  async predict() {
    const self = this;
    self.validationResults = [];

    self.isProcessing = true;

    let promise = self.question.id
      ? self.services
        .get(`${self.services.endpoints.learner}/${self.learnerId}/predict/${self.question.id}`)
      : self.services
        .post(`${self.services.endpoints.learner}/${self.learnerId}/predictCustom`, self.question);

    await promise
      .then((data: any) => {

        if (data.learnerId != self.learnerId || (self.question.id && data.questionId != self.question.id)) throw new Error();
        self.predictions = Object.assign(new Predictions(), data);

      })
      .catch(() => self.validationResults.push({ message: "Something went wrong; unable to fetch predictions." }))
      .then(() => { self.isProcessing = false; });
  }

  async showPerfData() {
    const self = this;

    if (self.learnerInfo) {
      self.isProcessing = true;
      let toProceed = true;
      const learnerInfo = self.learnerInfo;

      if (!learnerInfo.cumulativeFmeasures) {

        self.message = "Fetching data; this may take a while";
        toProceed = false;
        await self.services
          .get(`${self.services.endpoints.learner}/${self.learnerId}/fm`)
          .then((data: any) => {

            self.message = "Data received, now polishing data.";

            data = <{
              id: string, createdOn: string, preqGenFm: number[],
              preqTopkFm: number[],
              postTrainGenFm: number[],
              postTrainTopkFm: number[]
            }>data

            learnerInfo.cumulativeFmeasures = new CumulativeFmeasures(
              Utility.getCumulativeSumArray(data.preqGenFm), Utility.getCumulativeSumArray(data.preqTopkFm),
              Utility.getCumulativeSumArray(data.postTrainGenFm), Utility.getCumulativeSumArray(data.postTrainTopkFm));

          }).then(() => {
            toProceed = true;
          });
      }

      if (toProceed) {
        self.isProcessing = false;
        self.message = undefined;
        this.dialogService
          .open({ viewModel: FmChart, model: learnerInfo.cumulativeFmeasures, lock: false });
      }
    }
  }

  private async trainN(n: number) {
    const self = this;
    self.total = n;
    for (let i = 0; i < n; i++) {
      self.current = i + 1;

      try {

        await self.services
          .get(`${self.services.endpoints.learner}/${self.learnerId}/trainNext`)
          .then((data: any) => self.handlePostTrainingDataOnInstance(data));
      } catch (er) {
        self.validationResults.push({ message: "Something went wrong; unable to train." });
        break;
      }
    }
    self.total = 0;
  }

  private async trainOnCurrentQuestion() {
    const self = this;

    let promise = self.question.id
      ? self.services
        .get(`${self.services.endpoints.learner}/${self.learnerId}/train/${self.question.id}`)
      : self.services
        .post(`${self.services.endpoints.learner}/${self.learnerId}/trainCustom`, self.question);

    await promise
      .then((data: any) => self.handlePostTrainingDataOnInstance(data))
      .catch(() => self.validationResults.push({ message: "Something went wrong; unable to train." }))
      .then(() => { self.isProcessing = false; });
  }

  private handlePostTrainingDataOnInstance(data: any) {
    if (data.id != this.learnerId) throw new Error("Learner id does not match");
    if (this.learnerInfo == undefined) throw new Error("Something went wrong");
    this.learnerInfo.prequentialFmsCurrentInstance = data.prequentialFmsCurrentInstance;
    this.learnerInfo.postTrainingFmsCurrentInstance = data.postTrainingFmsCurrentInstance;
    this.learnerInfo.avgPrequentialFmeasures = data.avgPrequentialFmeasures;
    this.learnerInfo.avgPostTrainingFmeasures = data.avgPostTrainingFmeasures;
    this.learnerInfo.macroFmeasure = data.macroFmeasure;
  }
}
