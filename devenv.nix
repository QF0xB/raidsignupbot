{ pkgs, ... }:
{
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk25;
    maven = {
      enable = true;

    };
  };

  packages = [
    pkgs.git
    pkgs.sops
    pkgs.age
    pkgs.unzip
  ];
}
