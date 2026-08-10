# GitLab CI/CD YAML Keywords Reference

> Source: [GitLab CI/CD YAML syntax reference](https://docs.gitlab.com/ci/yaml/)

## Keywords

A GitLab CI/CD pipeline configuration includes:

### Global keywords that configure pipeline behavior

| Keyword | Description |
|---|---|
| `default` | Custom default values for job keywords. |
| `include` | Import configuration from other YAML files. |
| `stages` | The names and order of the pipeline stages. |
| `variables` | Define default CI/CD variables for all jobs in the pipeline. |
| `workflow` | Control what types of pipeline run. |

### Header keywords

| Keyword | Description |
|---|---|
| `spec` | Define specifications for external configuration files. |

### Jobs configured with job keywords

| Keyword | Description |
|---|---|
| `after_script` | Override a set of commands that are executed after job. |
| `allow_failure` | Allow job to fail. A failed job does not cause the pipeline to fail. |
| `artifacts` | List of files and directories to attach to a job on success. |
| `before_script` | Override a set of commands that are executed before job. |
| `cache` | List of files that should be cached between subsequent runs. |
| `coverage` | Code coverage settings for a given job. |
| `dast_configuration` | Use configuration from DAST profiles on a job level. |
| `dependencies` | Restrict which artifacts are passed to a specific job by providing a list of jobs to fetch artifacts from. |
| `environment` | Name of an environment to which the job deploys. |
| `extends` | Configuration entries that this job inherits from. |
| `identity` | Authenticate with third party services using identity federation. |
| `image` | Use Docker images. |
| `inherit` | Select which global defaults all jobs inherit. |
| `interruptible` | Defines if a job can be canceled when made redundant by a newer run. |
| `manual_confirmation` | Define a custom confirmation message for a manual job. |
| `needs` | Execute jobs earlier than the stage ordering. |
| `pages` | Upload the result of a job to use with GitLab Pages. |
| `parallel` | How many instances of a job should be run in parallel. |
| `release` | Instructs the runner to generate a release object. |
| `resource_group` | Limit job concurrency. |
| `retry` | When and how many times a job can be auto-retried in case of a failure. |
| `rules` | List of conditions to evaluate and determine selected attributes of a job, and whether or not it’s created. |
| `script` | Shell script that is executed by a runner. |
| `run` | Run configuration that is executed by a runner. |
| `secrets` | The CI/CD secrets the job needs. |
| `services` | Use Docker services images. |
| `stage` | Defines a job stage. |
| `start_in` | Delay job execution for a specified duration. Requires `when: delayed`. |
| `tags` | List of tags that are used to select a runner. |
| `timeout` | Define a custom job-level timeout that takes precedence over the project-wide setting. |
| `trigger` | Defines a downstream pipeline trigger. |
| `variables` | Define CI/CD variables for individual jobs. |
| `when` | When to run job. |
