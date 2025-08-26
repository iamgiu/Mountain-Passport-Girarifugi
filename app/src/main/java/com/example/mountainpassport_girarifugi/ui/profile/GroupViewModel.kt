package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupsViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // LiveData per i dati del gruppo
    private val _groupData = MutableLiveData<GroupData>()
    val groupData: LiveData<GroupData> = _groupData

    // LiveData per la lista dei membri
    private val _members = MutableLiveData<List<Member>>()
    val members: LiveData<List<Member>> = _members

    // LiveData per gestire errori di caricamento
    private val _loadingError = MutableLiveData<String>()
    val loadingError: LiveData<String> = _loadingError

    // LiveData per lo stato di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadGroupData(groupId: String) {
        // TODO: Carica i dati reali dal database Firebase
        // Per ora usa dati di esempio
        _isLoading.value = true

        loadSampleGroupMembers()
    }

    private fun loadSampleGroupMembers() {
        // Dati di esempio per i membri del gruppo
        val sampleMembers = listOf(
            Member(
                id = "member1",
                fullName = "Marco Rossi",
            ),
            Member(
                id = "member2",
                fullName = "Laura Bianchi",
            ),
            Member(
                id = "member3",
                fullName = "Giovanni Verdi",
            ),
            Member(
                id = "member4",
                fullName = "Sofia Neri",
            ),
            Member(
                id = "member5",
                fullName = "Alessandro Blu",
            )
        )

        _members.value = sampleMembers
        _isLoading.value = false
    }

    fun setGroupData(groupData: GroupData) {
        _groupData.value = groupData
    }

    fun refreshGroupData(groupId: String) {
        loadGroupData(groupId)
    }

    // Metodo per caricare i membri dal database (da implementare)
    /*fun loadGroupMembersFromDatabase(groupId: String) {
        _isLoading.value = true

        firestore.collection("groups").document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { documents ->
                _isLoading.value = false
                val membersList = mutableListOf<Member>()

                for (document in documents) {
                    try {
                        val member = document.toObject(Member::class.java)
                        membersList.add(member)
                    } catch (e: Exception) {
                        _loadingError.value = "Errore nel caricamento dei membri: ${e.message}"
                    }
                }

                _members.value = membersList
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _loadingError.value = "Errore di connessione: ${exception.message}"
                // Fallback ai dati di esempio
                loadSampleGroupMembers()
            }
    }*/

    // Metodo per caricare le statistiche del gruppo dal database
    /*fun loadGroupStatsFromDatabase(groupId: String) {
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // Carica le statistiche reali del gruppo
                        val stats = document.data
                        val groupData = GroupData(
                            name = stats?.get("name") as? String ?: "Gruppo",
                            username = "@${stats?.get("username") as? String ?: "gruppo"}",
                            monthlyScore = stats?.get("monthlyScore") as? String ?: "0",
                            visitedRefuges = stats?.get("visitedRefuges") as? String ?: "0"
                        )
                        _groupData.value = groupData
                    } catch (e: Exception) {
                        _loadingError.value = "Errore nel caricamento delle statistiche: ${e.message}"
                    }
                } else {
                    _loadingError.value = "Gruppo non trovato"
                }
            }
            .addOnFailureListener { exception ->
                _loadingError.value = "Errore di connessione: ${exception.message}"
            }
    }*/
}