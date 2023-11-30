import SigninPage from "views/Pages/signin/signin.component";
const SIGNIN_LAYOUT = "/signin";

function signinRoutes() {
  return [
    {
      path: "/",
      name: "Login Page",
      component: SigninPage,
      layout: SIGNIN_LAYOUT
    }
  ];
}

export default signinRoutes;
