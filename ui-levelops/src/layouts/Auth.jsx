import React, { Component } from "react";
import { Switch, Route } from "react-router-dom";

// dynamically create pages routes
import routes from "../routes/routes";

import bgImage from "assets/img/full-screen-image-3.jpg";

class Pages extends Component {
  componentWillMount() {
    if (document.documentElement.className.indexOf("nav-open") !== -1) {
      document.documentElement.classList.toggle("nav-open");
    }
  }

  getPageClass() {
    var pageClass = "";
    switch (this.props.location.pathname) {
      case "/auth/login-page":
        pageClass = " login-page";
        break;
      case "/auth/lock-screen-page":
        pageClass = " lock-screen-page";
        break;
      default:
        pageClass = " login-page";
        break;
    }
    return pageClass;
  }
  getRoutes = routes => routes
    .filter(route => route.layout === '/auth')
    .map((prop, key) => (
      <Route path={prop.layout + prop.path} component={prop.component} key={key} />
    ));

  render() {
    return (
      <div>
        <div className="flex wrapper wrapper-full-page">
          <div className={"full-page margin-auto" + this.getPageClass()} data-color="black" data-image={bgImage}>
            <div className="content">
              <Switch>{this.getRoutes(routes())}</Switch>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default Pages;
