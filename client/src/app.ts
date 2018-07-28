import { Aurelia } from 'aurelia-framework';
import { Router, RouterConfiguration } from 'aurelia-router';

export class App {
  router: Router;

  configureRouter(config: RouterConfiguration, router: Router) {
    config.title = 'Thesis';
    config.map([
      { route: ['', 'trainAndTest/:learnerId'], name: 'trainAndTest', moduleId: './trainAndTest', nav: true, title: 'Train/Test' },
      { route: ['create'], name: 'create', moduleId: './create', nav: true, title: 'Create Learner' },
      { route: ['stats'], name: 'stats', moduleId: './stats', nav: false, title: 'Stats' },
      // { route: 'users',         name: 'users',        moduleId: './users',        nav: true, title: 'Github Users' },
      // { route: 'child-router',  name: 'child-router', moduleId: './child-router', nav: true, title: 'Child Router' }
    ]);

    this.router = router;
  }
}
