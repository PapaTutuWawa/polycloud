{
  pre-commit,
  mkShell,
  nixfmt,
  jdk25,
  podman-compose,
  postgresql_16,
}:

mkShell {
  packages = [
    nixfmt
    jdk25
    podman-compose
    postgresql_16
  ]
  ++ pre-commit.enabledPackages;

  JAVA_HOME = jdk25.home;

  shellHook = ''
    ${pre-commit.shellHook}
    echo "polycloud dev shell (JDK ${jdk25.version})"
    echo "  ./gradlew :core:bootRun   - run the core service"
    echo "  podman compose up -d      - start postgres"
    echo "  nix build                 - build the core service as a package"
    echo "  nix run                   - build and run the core service"
  '';
}
