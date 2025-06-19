package util

import (
	"crypto/rand"
	"fmt"
	"gametracker/data"
)

func GameExists(arr *[]data.GameEntry, entry data.GameEntry) bool {
	for _, element := range *arr {
		if element.GameTitle == entry.GameTitle {
			return true
		}
	}
	return false
}

func EntryExists(arr *[]data.PlayEntry, entry data.PlayEntry) bool {
	for _, element := range *arr {
		if element.GameTitle == entry.GameTitle {
			return true
		}
	}
	return false
}

// GenerateID Generate a random id which is 8 bytes long
func GenerateID() (s string, err error) {
	b := make([]byte, 8)
	_, err = rand.Read(b)
	if err != nil {
		return
	}
	s = fmt.Sprintf("%x", b)
	return
}
