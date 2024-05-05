package util

import "gametracker/data"

// Check if game entry is already present.
func GameExists(arr *[]data.GameEntry, entry data.GameEntry) bool {
	for _, element := range *arr {
		if element.GameTitle == entry.GameTitle {
			return true
		}
	}
	return false
}

func EntryExists(arr []data.PlayEntry, entry data.PlayEntry) bool {
	for _, element := range arr {
		if element.GameTitle == entry.GameTitle {
			return true
		}
	}
	return false
}
