package util

import (
	"encoding/json"
	"os"
)

var GlobalConfig Config = Config{}

type Config struct {
	ServerPort          string
	MainJsonDbDirectory string
	PathToCertificate   string
	PathToPrivatekey    string
	DefaultDB           string
}

func (Config) ReadConfigFile() error {
	content, err := os.ReadFile("config.json")
	if err != nil {
		return err
	}

	json.Unmarshal(content, &GlobalConfig)

	return nil
}
