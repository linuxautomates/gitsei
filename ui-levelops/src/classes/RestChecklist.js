export class RestAdditionalData {

    constructor(restData=null) {
        this._name = undefined;
        this._value = undefined;
        if(restData !== null) {
            this._name = restData.name;
            this._value = restData.value;
        }
    }

    get name() { return this._name; }
    set name(name) { this._name = name; }

    get value() { return this._value; }
    set value(value) { this._value = value; }

    json() {
        return(
            {
                name: this._name,
                value: this._value
            }
        );
    }

    static validate(data) {
        let valid = true;
        valid = valid && data.hasOwnProperty("name");
        valid = valid && data.hasOwnProperty("value");
        return valid;
    }
}
export class RestCheck {

    constructor(restData=null) {
        this._id = undefined;
        this._name = undefined;
        this._description = undefined;
        this._checked = undefined;
        this._user_email = undefined;
        if(restData !== null) {
            this._id = restData.id;
            this._name = restData.name;
            this._description = restData.description;
            this._checked = restData.checked;
            this._user_email = restData.user_email;
        }
    }

    get id() { return this._id; }
    set id(id) { this._id = id; }

    get name() { return this._name; }
    set name(name) { this._name = name; }

    get description() { return this._description; }
    set description(desc) { this._description = desc; }

    get checked() { return this._checked; }
    set checked(checked) { this._checked = checked; }

    get user_email() { return this._user_email; }
    set user_email(email) { this._user_email = email; }

    json() {
        return(
            {
                id: this._id,
                name: this._name,
                description: this._description,
                checked: this._checked,
                user_email: this._user_email
            }
        );
    }

    static validate(data) {
        let valid = true;
        valid = valid && data.hasOwnProperty("id");
        valid = valid && data.hasOwnProperty("name");
        valid = valid && data.hasOwnProperty("description");
        return valid;
    }

}

export class RestChecklist {

    constructor(restData=null) {
        this._name = undefined;
        this._tags = [];
        this._id = undefined;
        this._work_item_id = undefined;
        this._completed = undefined;
        this._custom = undefined;
        this._script = undefined;
        this._additional_data = [];
        this._checks = [];
        this._artifact = undefined;
        this._integration_url = undefined;
        this._integration_application = undefined;
        if (restData !== null)  {
            this._id = restData.id;
            this._name = restData.name;
            this._tags = restData.tags;
            this._completed = restData.completed;
            this._custom = restData.custom;
            this._script = restData.script;
            this._integration_url = restData.integration_url;
            this._integration_application = restData.integration_application;
            this._work_item_id = restData.work_item_id;
            this._artifact = restData.artifact;
            if(restData.hasOwnProperty("checks")) {
                restData.checks.forEach(
                    check => this._checks.push(new RestCheck(check))
                )
            }
            if(restData.hasOwnProperty("additional_data")) {
                restData.additional_data.forEach(
                    data => this._additional_data.push(new RestAdditionalData(data))
                )
            }
        }
    }

    get id() { return this._id; }
    set id(id) { this._id = id; }

    get tags() { return this._tags; }
    set tags(tags) { this._tags = tags; }

    get name() { return this._name; }
    set name(name) { this._name = name; }

    get completed() { return this._completed; }
    set completed(completed) { this._completed = completed; }

    get custom() { return this._custom; }
    set custom(custom) { this._custom = custom; }

    get script() { return this._script; }
    set script(script) { this._script = script; }

    get checks() { return this._checks; }
    set checks(checks) { this._checks = checks; }

    get additional_data() { return this._additional_data; }
    set additional_data(data) { this._additional_data = data; }

    get artifact() { return this._artifact; }
    set artifact(art) { this._artifact = art; }

    get integration_url() { return this._integration_url; }
    set integration_url(url) { this._integration_url = url; }

    get integration_application() { return this._integration_application; }
    set integration_application(app) { this._integration_application = app; }

    json() {
        return(
            {
                id: this._id,
                name: this._name,
                tags: this._tags,
                script: this._script,
                custom: this._custom,
                completed: this._completed,
                artifact: this._artifact,
                integration_url: this._integration_url,
                integration_application: this._integration_application,
                checks: this._checks.map(check => check.json()),
                additional_data: this._additional_data.map(data => data.json())
            }
        );
    }

    static validate(data) {
        let valid = true;
        valid = valid && data.hasOwnProperty("id");
        valid = valid && data.hasOwnProperty("name");
        valid = valid && data.hasOwnProperty("tags");
        valid = valid && data.hasOwnProperty("completed");
        valid = valid && data.hasOwnProperty("script");
        valid = valid && data.hasOwnProperty("checks") && data.checks.forEach(
            check => {
                valid = valid && RestCheck.validate(check)
            }
        );
        valid = valid && data.hasOwnProperty("additional_data") && data.additional_data.forEach(
            data => {
                valid = valid && RestAdditionalData.validate(data)
            }
        );
        return valid;
    }
}