package ru.iopump.qa.allure.service.generate;

public class IINRecord {
    private String iin;
    // Для физлиц
    private String sex;
    private String birthday;
    private String resident;
    private String name;
    private String surname;
    private String middleName;
    // Для юрлиц
    private String typeEntity;
    private String typeOrg;
    private String dateReg;
    private String nameCom;

    // Поля для результатов создания
    private String clientCardResult;
    private String clientAccountResult; // Новое поле для номера(ов) счета

    public String getIin() {
        return iin;
    }
    public void setIin(String iin) {
        this.iin = iin;
    }
    public String getSex() {
        return sex;
    }
    public void setSex(String sex) {
        this.sex = sex;
    }
    public String getBirthday() {
        return birthday;
    }
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
    public String getResident() {
        return resident;
    }
    public void setResident(String resident) {
        this.resident = resident;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSurname() {
        return surname;
    }
    public void setSurname(String surname) {
        this.surname = surname;
    }
    public String getMiddleName() {
        return middleName;
    }
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    public String getTypeEntity() {
        return typeEntity;
    }
    public void setTypeEntity(String typeEntity) {
        this.typeEntity = typeEntity;
    }
    public String getTypeOrg() {
        return typeOrg;
    }
    public void setTypeOrg(String typeOrg) {
        this.typeOrg = typeOrg;
    }
    public String getDateReg() {
        return dateReg;
    }
    public void setDateReg(String dateReg) {
        this.dateReg = dateReg;
    }
    public String getNameCom() {
        return nameCom;
    }
    public void setNameCom(String nameCom) {
        this.nameCom = nameCom;
    }
    public String getClientCardResult() {
        return clientCardResult;
    }
    public void setClientCardResult(String clientCardResult) {
        this.clientCardResult = clientCardResult;
    }
    public String getClientAccountResult() {
        return clientAccountResult;
    }
    public void setClientAccountResult(String clientAccountResult) {
        this.clientAccountResult = clientAccountResult;
    }
}
