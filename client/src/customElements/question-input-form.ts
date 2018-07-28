import { bindable, inject, NewInstance } from "aurelia-framework";
import { ValidationController, ValidationRules, Rule } from "aurelia-validation";
import { EventAggregator } from 'aurelia-event-aggregator';
import { Services } from '../core/Services';
import promisify from "es6-promisify";
// import {promisify} from "es6-promisify";

@inject(Services, EventAggregator, NewInstance.of(ValidationController))
export class QuestionInputForm {
  @bindable question: any;
  soQuestionId: number;
  title: string;
  body: string;
  tags: string[] = [];
  newTag: string;
  validationResults: any[] = [];

  soQuestionIdRule: Rule<QuestionInputForm, any>[][];
  questionRule: Rule<QuestionInputForm, any>[][];

  constructor(private services: Services, private eventAggregator: EventAggregator, private validationController: ValidationController) {

    this.soQuestionIdRule = ValidationRules
      .ensure((vm: QuestionInputForm) => vm.soQuestionId)
      .displayName("Stack Overflow Question Id")
      .required()
      .rules;

    this.questionRule = ValidationRules
      .ensure((vm: QuestionInputForm) => vm.title).displayName("Question's title").required().minLength(1)
      .ensure((vm: QuestionInputForm) => vm.body).displayName("Question's body").required().minLength(1)
      .ensure((vm: QuestionInputForm) => vm.tags).displayName("Question's tags").minItems(1)
      .rules;

    const stat = promisify(this.addTag, undefined);
  }

  async fetchQuestion() {
    const self = this;
    let isValid = false;

    await self.validationController
      .validate({ object: self, rules: self.soQuestionIdRule })
      .then(vr => {
        self.validationResults = vr.results;
        isValid = vr.valid;
      });
    if (isValid) {
      await self.services
        .get(`${self.services.endpoints.question}/soid/${self.soQuestionId}/`)
        .then((data: any) => {
          if (!data) { alert("Question not found. Try another question id."); }
          else { self.setQuestion(data); }
        }).catch(reason => alert("Question not found. Try another question id."));
    }
  }

  addTag() {
    this.tags.push(this.newTag);
    this.newTag = "";
  }

  deleteTag(index: number) {
    this.tags.splice(index, 1);
  }

  async createQuestion() {
    const self = this;
    let isValid = false;

    await self.validationController
      .validate({ object: self, rules: self.questionRule })
      .then(vr => {
        self.validationResults = vr.results;
        isValid = vr.valid;
      });

    if (isValid)
      this.setQuestion({
        title: this.title,
        body: this.body,
        tags: this.tags.map((tag) => { return { name: tag }; })
      });
  }

  setQuestion(question: any) {
    this.question = question;
    this.eventAggregator.publish('QuestionInput:QuestionCreated');
  }
}
