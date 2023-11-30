import React from "react";
import * as PropTypes from "prop-types";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntCardComponent as AntCard } from "shared-resources/components/ant-card/ant-card.component";
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
import "./step-progress.style.scss";

export class StepProgressComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      currentStep: props.startAtStep
    };

    this.onChangeStepHandler = this.onChangeStepHandler.bind(this);
    this.onSkipToStepHandler = this.onSkipToStepHandler.bind(this);
  }

  onChangeStepHandler(increment) {
    return () => {
      this.props.steps[this.state.currentStep].onSubmitStep();
      this.setState(state => ({
        currentStep: state.currentStep + increment
      }));
    };
  }

  onSkipToStepHandler(stepIndex) {
    return () => {
      const requiredIndex = this.props.steps.findIndex(step => step.isRequired);
      const isAccessible = requiredIndex > -1 && requiredIndex < stepIndex;

      if (isAccessible) {
        this.setState({
          currentStep: stepIndex
        });
      }
    };
  }

  render() {
    const { className, steps } = this.props;
    const { currentStep } = this.state;
    const stepsCount = steps.length;
    return (
      <div className={`${className} flex direction-column`}>
        <div className={`${className}__navigate ${this.props.extraClass} flex justify-end`}>
          {this.props.canGoToPrevious && currentStep >= 1 && (
            <AntButton type="primary" onClick={this.onChangeStepHandler(-1)}>
              Previous Step
            </AntButton>
          )}
          {currentStep !== stepsCount - 1 && (
            <AntButton
              disabled={steps[currentStep].isNextDisabled}
              type="primary"
              onClick={this.onChangeStepHandler(1)}>
              Next Step
            </AntButton>
          )}
          {currentStep === stepsCount - 1 && (
            <AntButton type={"primary"} onClick={this.props.onFinishEvent}>
              Finish
            </AntButton>
          )}
        </div>
        <div className={`${className}__progress-bar flex direction-row align-center`}>
          {steps.map((step, index) => (
            <div
              key={`step-${index}`}
              className={`${className}__step flex direction-column`}
              style={{ flexBasis: index < stepsCount - 1 ? `${100 / (stepsCount - 1)}%` : "" }}>
              <span className={`${className}__step-label ${className}__step-label--${step.status}`}>{step.label}</span>
              <div className="flex align-center">
                <div
                  onClick={this.onSkipToStepHandler(index)}
                  className={`${className}__circle ${className}__circle--${step.status}`}
                  role="presentation">
                  {step.status === "done" && <SvgIcon icon="check" />}
                </div>
                {index !== stepsCount - 1 && (
                  <span className={`${className}__line ${className}__line--${step.status}`} />
                )}
              </div>
            </div>
          ))}
        </div>
        <div
          className={`${className}__content flex ${
            !this.props.hasPreview || !this.props.previewItems.length ? "justify-center" : ""
          }`}>
          {this.props.hasPreview && !!this.props.previewItems.length && (
            <div className={`${className}__preview`}>
              <AntCard className="mr-20">
                {this.props.previewItems.map(item => (
                  <div key={`item-${item.value}`}>
                    {item.hasIcon && (
                      <div className="flex direction-row justify-start">
                        <SvgIcon style={{ width: "2rem", height: "2rem" }} icon={item.icon} />
                        <div style={{ marginLeft: "5px" }}>{item.value}</div>
                      </div>
                    )}
                    {!item.hasIcon && (
                      <div className={`${className}__item flex direction-column`}>
                        <div className={`${className}__item-label`}>{item.label}</div>
                        <div className={`${className}__item-value`}>{item.value}</div>
                      </div>
                    )}
                  </div>
                ))}
              </AntCard>
            </div>
          )}
          <div className={`${className}__current-step-content`}>
            {!steps[currentStep].disabled && steps[currentStep].component}
          </div>
        </div>
      </div>
    );
  }
}

StepProgressComponent.propTypes = {
  className: PropTypes.string,
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
      status: PropTypes.oneOf(["done", "inProgress", "toDo"]).isRequired,
      component: PropTypes.node.isRequired,
      onSubmitStep: PropTypes.func.isRequired,
      isRequired: PropTypes.bool,
      label: PropTypes.string,
      isNextDisabled: PropTypes.bool
    })
  ),
  onFinishEvent: PropTypes.func.isRequired,
  hasPreview: PropTypes.bool,
  canGoToPrevious: PropTypes.bool,
  startAtStep: PropTypes.number,
  extraClass: PropTypes.string,
  previewItems: PropTypes.array
};

StepProgressComponent.defaultProps = {
  className: "step-progress",
  hasPreview: true,
  canGoToPrevious: false,
  startAtStep: 0,
  steps: [],
  extraClass: "",
  previewItems: []
};
