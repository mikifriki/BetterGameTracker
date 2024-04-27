package util

import "gametracker/data"

func StructExists(arr []data.GameEntry, entry data.GameEntry) bool {
	for _, element := range arr {
		if element.Title == entry.Title {
			return true
		}
	}
	return false
}
