fields{
  required{
    registry = String
    cred = String
  }
  optional{
    build_strategy = ["dockerfile", "docker-compose", "modules"]
    repo_path_prefix = String
  }
}
