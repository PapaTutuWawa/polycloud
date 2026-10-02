{
  stdenv,
  jdk25,
  gradle_9,
  lib,
  makeWrapper,
}:

let
  jdk = jdk25;
  gradle = gradle_9;

  # core/build.gradle.kts only pulls these in via `developmentOnly`, so
  # core's own bootJar never bundles them. We build them separately here
  # and stitch them onto the runtime classpath ourselves, without
  # touching the project's Gradle config.
  plugins = [
    "authOidc"
    "files"
    "calendar"
  ];

  pluginBuildTasks = lib.concatMapStringsSep " " (p: ":${p}:jar :${p}:nixExportRuntimeLibs") plugins;

  pluginInstallLines = lib.concatMapStringsSep "\n" (p: ''
    cp plugins/${p}/build/libs/*.jar $out/share/polycloud/plugins/
    if [ -d plugins/${p}/build/nix-runtime-libs ]; then
      cp -n plugins/${p}/build/nix-runtime-libs/*.jar $out/share/polycloud/plugin-libs/
    fi
  '') plugins;
in
stdenv.mkDerivation (finalAttrs: {
  pname = "polycloud";
  version = "0.0.1-SNAPSHOT";

  src = lib.fileset.toSource {
    root = ../../..;
    fileset = lib.fileset.gitTracked ../../..;
  };

  nativeBuildInputs = [
    gradle
    jdk
    makeWrapper
  ];

  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  gradleFlags = [
    "-Porg.gradle.java.installations.paths=${jdk}"
    "-Porg.gradle.java.installations.auto-detect=false"
    "-Porg.gradle.java.installations.auto-download=false"
    "--init-script=${../../export-runtime-libs.init.gradle}"
  ];

  gradleBuildTask = ":core:bootJar ${pluginBuildTasks}";

  installPhase = ''
    runHook preInstall

    mkdir -p $out/share/polycloud/app $out/share/polycloud/plugins $out/share/polycloud/plugin-libs

    # Spring's default JarLauncher (see core's bootJar manifest) can't be
    # extended with external classpath entries, so explode the fat jar
    # and assemble the classpath ourselves instead of using `-jar`.
    bootJar=$(readlink -f core/build/libs/*.jar)
    (cd $out/share/polycloud/app && ${jdk}/bin/jar xf "$bootJar")
    startClass=$(sed -n 's/^Start-Class: *//p' $out/share/polycloud/app/META-INF/MANIFEST.MF | tr -d '\r')

    ${pluginInstallLines}

    makeWrapper ${jdk}/bin/java $out/bin/polycloud \
      --add-flags "-cp $out/share/polycloud/app/BOOT-INF/classes:$out/share/polycloud/app/BOOT-INF/lib/*:$out/share/polycloud/plugins/*:$out/share/polycloud/plugin-libs/*" \
      --add-flags "$startClass"

    runHook postInstall
  '';

  meta.mainProgram = "polycloud";
})
